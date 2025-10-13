# CMMS11 STRUCTURES

**Controller 분리 · SPA Form · 결재/권한 · 회사선택 로그인 · 번호규칙 · 파일 API · 초기데이터 통합 표준**

**Final++++++++ Revision 2025-10-13**

---

## 📘 목차

1. [개요](#1-개요)
2. [시스템 구조](#2-시스템-구조)
3. [로그인 및 인증](#3-로그인-및-인증)
4. [SPA 구조](#4-spa-구조)
5. [Controller 분리](#5-controller-분리)
6. [Form 처리](#6-form-처리)
7. [MemberService](#7-memberservice)
8. [결재 및 콜백](#8-결재-및-콜백)
9. [Service / Repository 표준](#9-service--repository-표준)
10. [권한(RBAC)](#10-권한rbac)
11. [번호 규칙 (PREFIX 포함)](#11-번호-규칙-prefix-포함)
12. [코드값 (Seed 기본코드)](#12-코드값-seed-기본코드)
13. [파일 API 엔드포인트](#13-파일-api-엔드포인트)
14. [초기 데이터 (DataInitializer)](#14-초기-데이터-datainitializer)
15. [UI·CSS](#15-uicss)
16. [개발 규칙](#16-개발-규칙)
17. [향후 계획](#17-향후-계획)

---

## 1. 개요

- **Page / API Controller 분리**
- **업무모듈** → SPA(data-form-manager), **마스터** → POST
- **회사 선택 로그인**(companyId:username)
- **결재 Stage/Status 표준화**
- **파일 업로드 REST API 통합**
- **초기 데이터 자동 Seed 적용**

---

## 2. 시스템 구조

```
cmms11/
├─ web/page, web/api
├─ domain, plant, inventory
├─ inspection, workorder, workpermit
├─ approval, memo, file
```

- 모든 테이블에 `company_id CHAR(5)` 선행.

---

## 3. 로그인 및 인증

- 로그인 폼에서 **회사 선택** + ID/PW 입력
- `username = C0001:admin` 형식
- `MemberUserDetailsService`가 분리(split(':')) → DB 조회
- 성공 시 `sessionInfo` 생성 (companyId, memberId 등)

---

## 4. SPA 구조

```
assets/js/
├─ core/ (csrf, navigation)
├─ api/ (auth, storage)
├─ ui/ (file-upload, file-list)
└─ pages/ (inspection.js 등)
```

- `layout.html` 기반 fragment fetch + `navigation.js` 라우팅 지원.

---

## 5. Controller 분리

| 유형 | 모듈 | 구조 |
|------|------|------|
| 업무 | Inspection, WorkOrder, WorkPermit, Memo, Approval | Page + API |
| 마스터 | Company, Dept, Plant, Inventory, Code | 단일(POST) |
| 시스템 | Auth, Layout, Health | 단일 |

---

## 6. Form 처리

| 구분 | 방식 | 설명 |
|------|------|------|
| 도메인·마스터 | HTML POST | 서버 검증 후 redirect |
| 업무모듈 | SPA(fetch JSON) | 비동기 저장 후 redirect |

### 예시

```html
<form data-form-manager 
      data-action="/api/workorders" 
      data-method="POST" 
      data-redirect="/workorder/detail/{id}">
</form>
```

---

## 7. MemberService

- `findByCompanyIdAndMemberId()` 조회
- 로그인 및 내정보 수정 담당

```java
public Member getExisting(String c, String m) {
  return repo.findByCompanyIdAndMemberId(c, m)
    .orElseThrow(() -> new IllegalArgumentException("No member"));
}
```

---

## 8. 결재 및 콜백

### 8-1. Stage & Status

- **Stage**: `PLN`, `ACT`
- **Status**: `DRAFT`, `SUBMT`, `PROC`, `APPRV`, `REJCT`, `CMPLT`

| 상태 | 설명 |
|------|------|
| DRAFT | 임시저장 (기안) |
| SUBMT | 상신 (SUBMIT) |
| PROC | 진행 (PROCESS) |
| APPRV | 승인 (APPROVE) |
| REJCT | 반려 (REJECT) |
| CMPLT | 결재 없이 확정 (COMPLETE) |

### 8-2. ApprovalStep 필드

- **decision**: 결재 역할 (`APPRL`, `AGREE`, `INFO`)
- **result**: 결재 결과 (`APPROVE`, `REJECT`, `NULL`)
- **ref_entity**: `INSP` / `WORK` / `WPER`
- **ref_id**: 원문서 ID
- **ref_stage**: `PLN` / `ACT`

### 8-3. 결재 흐름

결재 요청 → Approval 생성 → 승인/반려 시 원 모듈 Service 콜백 → status 자동 변경  
(모든 처리 `@Transactional`)

### 8-4. 모듈별 결재 메서드 표준

| 모듈 | 계획단계 | 실적단계 |
|------|----------|----------|
| **Inspection** | `submitPlanApproval`<br>`onPlanApprovalApprove/Reject/Delete/Complete`<br>`buildPlanApprovalContent`<br>`prepareActualStage` | `submitActualApproval`<br>`onActualApprovalApprove/Reject/Delete/Complete`<br>`buildActualApprovalContent` |
| **WorkOrder** | `submitPlanApproval`<br>`onPlanApprovalApprove/Reject/Delete/Complete`<br>`buildPlanApprovalContent`<br>`prepareActualStage` | `submitActualApproval`<br>`onActualApprovalApprove/Reject/Delete/Complete`<br>`buildActualApprovalContent` |
| **WorkPermit** | `submitPlanApproval`<br>`onPlanApprovalApprove/Reject/Delete/Complete`<br>`buildPlanApprovalContent` | – |

---

## 9. Service / Repository 표준

### Service 메서드

```java
// 기본 CRUD
list(), get(), create(), update(), delete()

// 결재 관련
submitPlanApproval(), onPlanApprovalApprove(), prepareActualStage()

// 유틸리티
applyRequest(), resolveId(), currentMemberId()
```

### Repository 메서드

```java
Page<T> findByCompanyId(String c, Pageable p);
Optional<T> findByCompanyIdAndId(String c, String id);
```

---

## 10. 권한(RBAC)

| 역할 | 권한 | 예시 |
|------|------|------|
| ADMIN | 전체 CRUD | – |
| MANAGER | 업무 CRUD | INSPECTION_U |
| ASSISTANT | 실적 작성 | WORKORDER_U |
| VIEWER | 조회 전용 | INSPECTION_R |

### 사용 예시

```java
@PreAuthorize("hasAuthority('WORKORDER_U')")
```

---

## 11. 번호 규칙 (PREFIX 포함)

### 11-1. Master ID (기준정보)

- **형식**: `{moduleCode(1)}{000000}{3자리시퀀스}`
- **API**: `generateMasterId(companyId, moduleCode)`

| 모듈 | 코드 | 예시 |
|------|------|------|
| Plant(설비) | 1 | 1000000001 |
| Inventory(재고) | 2 | 2000000001 |

### 11-2. Transaction ID (업무데이터)

- **형식**: `{moduleCode(1)}{YYMMDD}{3자리시퀀스}`
- **API**: `generateTxId(companyId, moduleCode, date)`

| 모듈 | 코드 | 예시 |
|------|------|------|
| Inspection(점검) | I | I250119001 |
| WorkOrder(작업지시) | W | W250119001 |
| WorkPermit(작업허가) | P | P250119001 |
| Approval(결재) | A | A250119001 |
| File(파일) | F | F250119001 |
| Memo(게시글) | M | M250119001 |

---

## 12. 코드값 (Seed 기본코드)

### 12-1. 모듈 코드 (MODUL)

```java
seedItems("MODUL", List.of(
    new SeedCodeItem("PLANT", "설비"),
    new SeedCodeItem("INVET", "재고"),
    new SeedCodeItem("INSP", "점검"),
    new SeedCodeItem("WORK", "작업지시"),
    new SeedCodeItem("WPER", "작업허가"),
    new SeedCodeItem("MEMO", "게시글"),
    new SeedCodeItem("APPRL", "결재")
));
```

### 12-2. 결재 상태 코드 (APPRV)

```java
seedItems("APPRV", List.of(
    new SeedCodeItem("DRAFT", "기안"),
    new SeedCodeItem("SUBMT", "제출(SUBMIT)"),
    new SeedCodeItem("PROC", "처리중(PROCESS)"),
    new SeedCodeItem("APPRV", "승인(APPROVE)"),
    new SeedCodeItem("REJCT", "반려(REJECT)"),
    new SeedCodeItem("CMPLT", "결재없이확정건(COMPLETE)")
));
```

### 12-3. 결재 역할 코드 (DECSN)

```java
seedItems("DECSN", List.of(
    new SeedCodeItem("APPRL", "결재(APPROVAL)"),
    new SeedCodeItem("AGREE", "합의(AGREE)"),
    new SeedCodeItem("INFO", "참조(INFORM)")
));
```

---

## 13. 파일 API 엔드포인트

| 기능 | Method / URL | 요청 | 응답 |
|------|--------------|------|------|
| **업로드** | `POST /api/files` | `Content-Type: multipart/form-data`<br>`files[]`, `refEntity?`, `refId?` | `{"fileGroupId":"F250107001","items":[...]}` |
| **목록 조회** | `GET /api/files?groupId={fileGroupId}` | – | `{"fileGroupId":"...","items":[...]}` |
| **다운로드** | `GET /api/files/{fileId}?groupId={fileGroupId}` | – | 파일 바이너리<br>`Content-Disposition: attachment` |
| **삭제** | `DELETE /api/files/{fileId}?groupId={fileGroupId}` | – | `204 No Content` |

### DB 스키마

- `file_group`, `file_item` 테이블
- 각 엔드포인트는 `FileController`에서 REST 기반으로 제공됨.

---

## 14. 초기 데이터 (DataInitializer)

- `MODUL`, `APPRV`, `DECSN` 등 Seed 코드와 기초 데이터를 자동 주입
- 테스트용 회사·부서·사용자·코드값 동시 생성
- 결재 워크플로우 테스트 시 `DataInitializer`가 Stage/Status 값을 사전 입력하여 전이 테스트를 지원

---

## 15. UI·CSS

- CSS 변수 기반 컬러·폰트(`base.css`)
- `.grid.cols-12`, `.form-row`, `.card`
- 반응형 (≤768px) 1열 구조
- `print.css` 별도 관리

---

## 16. 개발 규칙

| 항목 | 규칙 |
|------|------|
| Controller | `<Module>PageController`, `<Module>ApiController` |
| Service | `<Module>Service` |
| Repository | `<Module>Repository` |
| Entity | PascalCase |
| JS | kebab-case |
| CSS | hyphen-case |
| Package | 기능별 colocation |

---

## 17. 향후 계획

- SSO + API Gateway 도입
- NAS → S3 전환
- KPI Dashboard 및 PIMS 연계
- 모바일 UI (2026 상반기)

---

## ✅ 최종 상태

| 항목 | 완료 |
|------|------|
| Controller 분리 | ✅ |
| Form 구조 | ✅ |
| 로그인 구조 | ✅ |
| Approval 콜백 | ✅ |
| RBAC | ✅ |
| ID/Code 규칙 | ✅ |
| Build | ✅ SUCCESS (2025-10-13) |

---

## 📚 참조 문서

- [CMMS_PRD.md](CMMS_PRD.md)
- [CMMS_JAVASCRIPT.md](CMMS_JAVASCRIPT.md)
- [CMMS_TABLES.md](CMMS_TABLES.md)
- [CMMS_CSS.md](CMMS_CSS.md)
- [MIGRATION_PLAN.md](MIGRATION_PLAN.md)
- [MIGRATION_APPROVAL.md](MIGRATION_APPROVAL.md)

---

**이 문서는 CMMS11의 결재 상태코드(SUBMT, APPRV 등)와 Master/Transaction ID 생성 규칙, Seed 기본코드를 모두 포함한 최신 표준 사양 문서이며, 개발·운영 시스템의 공식 참조 버전입니다.**
