# CMMS11 STRUCTURES

**Controller 분리 · SPA Form · 결재/권한 · 회사선택 로그인 · 번호규칙 · 파일 API · 초기데이터 통합 표준**

**Final++++++++ Revision 2025-10-15**

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

- **Page / API Controller 완전 분리**
- **업무모듈 + 마스터 데이터** → PageController + ApiController 구조
- **도메인(외부 참조)** → 단일 Controller + 선택적 GET API
- **도메인(내부 관리)** → 단일 Controller + POST만
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
│   ├─ *ApprovalService (모듈 결재 상신)
│   ├─ *ApprovalWebhookController (Webhook 수신)
│   └─ Service (상태 전이/실적 준비)
├─ approval
│   ├─ ApprovalService (REST + Outbox)
│   ├─ client/ApprovalClient
│   ├─ ApprovalOutboxScheduler
│   └─ WebhookIdempotencyRepository
├─ memo, file
```

- 모든 테이블에 `company_id CHAR(5)` 선행.
- **결재 연계**: Approval REST API + Outbox/Webhook + 모듈별 ApprovalService (2025-10-21)

---

## 3. 로그인 및 인증

### 기본 흐름

- 로그인 폼에서 **회사 선택** + ID/PW 입력
- `username = C0001:admin` 형식
- `MemberUserDetailsService`가 분리(split(':')) → DB 조회
- 성공 시 `sessionInfo` 생성 (companyId, memberId 등)

### 회사 목록 동적 로딩

**LoginController** (`LoginController.java`):
- `GET /auth/login.html` 요청 시 활성 회사 목록 조회
- `Company` 테이블에서 `delete_mark='N'` 조건으로 필터링
- companyId 오름차순 정렬하여 모델에 추가

**login.html** (Thymeleaf 동적 렌더링):
```html
<select id="company_id" class="input" required>
  <option value="">회사를 선택하세요</option>
  <option th:each="company : ${companies}" 
          th:value="${company.companyId}"
          th:text="${company.name + ' (' + company.companyId + ')'}">
  </option>
</select>
```

**DataInitializer** (초기 데이터):
- 3개 회사 자동 생성: `CHROK`, `HPS`, `KEPS`
- 각 회사별 admin 계정 생성 (비밀번호: `1234`)
- 로그인: `CHROK → admin`, `HPS → HPS:admin`, `KEPS → KEPS:admin`

### 보안 검증 (2025-10-13 강화)

**프론트엔드** (`login.html`):
1. `:` 문자 입력 차단 (구분자 보호)
2. 회사 선택 필수 검증
3. 클라이언트 측 에러 메시지 표시

**백엔드** (`MemberUserDetailsService`):
1. 입력값 null/empty 검증
2. `:` 구분자 파싱 후 양쪽 공백 제거 및 검증
3. 다중 `:` 차단 (우회 방지)
4. **통일된 에러 메시지**: 모든 실패 시 "Invalid credentials" (계정 존재 유무 노출 방지)

**실패 처리** (`SecurityConfig`):
- 모든 로그인 실패를 동일한 URL로 리다이렉트
- 사용자에게 표시: "아이디 또는 비밀번호가 일치하지 않습니다."
- 서버 로그에만 실패 원인 기록 (감사 목적)

**입력 허용 범위**:
- ✅ 한글: `홍길동`, `관리자`
- ✅ 이메일 형식: `admin@company`
- ✅ 특수문자: `admin-01`, `user.test`
- ❌ 콜론만 차단: `admin:test` (구분자 충돌)

### 로그인 정보 저장 및 추적 (2025-10-14 추가)

**로그인 정보 저장 (쿠키)**:
- 사용자가 "로그인 정보 저장" 체크박스를 선택하면 30일 유지 쿠키 생성
- 쿠키: `cmms_company_id`, `cmms_username`, `cmms_remember`
- 다음 로그인 시 LoginController가 쿠키를 읽어서 폼 자동 완성
- 체크박스 상태도 복원됨 (Thymeleaf `th:checked`)

**마지막 로그인 추적**:
- Member 테이블에 `last_login_at`, `last_login_ip` 필드 추가
- 로그인 성공 시 SecurityConfig.successHandler에서 자동 업데이트
- IP 주소는 프록시 헤더 고려 (X-Forwarded-For, X-Real-IP 등)
- LayoutController에서 사용자 정보 조회 시 마지막 로그인 정보를 모델에 추가
- defaultLayout.html 헤더에 "이전 로그인: 2025-10-14 09:30 (192.168.1.100)" 형식으로 표시

**세션 타임아웃**:
- 기본값: 1시간 (`application.yml`: `server.servlet.session.timeout: 1h`)
- 세션 쿠키: HttpOnly=true, Secure=false (dev), Secure=true (prod)
- 자동 연장: 사용자가 페이지 이동, 검색, API 호출 등 모든 HTTP 요청 시 마지막 접근 시간부터 다시 1시간 자동 연장됨 (Spring Security 기본 동작)

**로그아웃 시 쿠키 처리**:
- 로그아웃 시 JSESSIONID 삭제
- "로그인 정보 저장" 쿠키는 유지 (30일 만료 또는 명시적 체크 해제 시 삭제)

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

### 5-1. 전체 구조 (2025-10-15 최신)

| 계층 | 모듈 | 구조 | PageController | ApiController |
|------|------|------|---------------|---------------|
| **업무** | Inspection, WorkOrder, WorkPermit<br>Memo, Approval, InventoryTx | **Page + API 완전 분리** | 화면 렌더링<br>POST 처리 | GET/POST/PUT/DELETE<br>전체 CRUD API |
| **마스터 데이터** | **Plant, Inventory** | **Page + API 완전 분리** | 화면 렌더링<br>POST 처리 | GET/POST/PUT/DELETE<br>+ upload API |
| **도메인(외부 참조)** | **Dept, Func, Member** | **단일 + 선택적 API** | 화면 렌더링<br>POST 처리 | **GET만** (picker용) |
| **도메인(내부 관리)** | Company, Site, Role, Storage | 단일(POST 전용) | 화면 렌더링<br>POST 처리 | ❌ 없음 |
| **코드** | Code | 단일(POST 전용) | 화면 렌더링<br>POST 처리 | ❌ 없음 |
| **시스템** | Login, Layout, Health | 단일 | 화면 렌더링 | – |
| **시스템 API** | Auth | REST API 전용 | – | 인증 API |

### 5-2. 상세 설명

#### 업무 모듈 (PageController + ApiController)
```
web/page/InspectionPageController.java    → 화면 전용
web/api/InspectionApiController.java      → REST API 전용
```

**PageController 역할**:
- `@GetMapping("/inspection/list")` - 목록 화면
- `@GetMapping("/inspection/form")` - 등록/수정 폼
- `@GetMapping("/inspection/detail/{id}")` - 상세 화면
- `emptyObject()` - 빈 객체 생성
- `addReferenceData()` - Select box용 참조 데이터

**ApiController 역할**:
- `@GetMapping` - 조회
- `@PostMapping` - 생성
- `@PutMapping` - 수정
- `@DeleteMapping` - 삭제

#### 마스터 데이터 (PageController + ApiController)
```
web/page/PlantPageController.java         → 화면 전용
web/api/PlantApiController.java           → REST API 전용
```

**특징**: 업무 모듈과 **완전히 동일한 구조**
- 화면: POST 방식으로 CRUD
- API: 전체 REST API 제공 (picker, 대량 업로드 포함)

#### 도메인(외부 참조) - 선택적 GET API
```
web/DeptController.java                   → 단일 Controller
```

**특징**: 
- 화면: POST 방식으로 CRUD
- API: **조회(GET)만 제공** (picker, 참조 데이터용)
- POST/PUT/DELETE API는 제공하지 않음

**API 제공 이유**:
- `Dept` → org-picker.html에서 사용
- `Func` → plant-picker.html에서 사용
- `Member` → org-picker.html, approval.js에서 사용

#### 도메인(내부 관리) - POST만
```
web/CompanyController.java                → 단일 Controller (POST 전용)
```

**특징**: 
- 화면: POST 방식으로 CRUD
- API: 제공하지 않음 (내부 관리만, 외부 참조 없음)

---

## 6. Form 처리

### 6-1. 모듈별 Form 처리 방식

| 계층 | 모듈 | 화면 저장 방식 | API 사용 |
|------|------|---------------|---------|
| 업무 | Inspection, WorkOrder, WorkPermit, Memo, Approval, InventoryTx | **SPA(fetch JSON)** | ✅ ApiController |
| 마스터 | Plant, Inventory | **HTML POST** | ✅ ApiController (picker, upload) |
| 도메인(외부) | Dept, Func, Member | **HTML POST** | ✅ GET만 (picker) |
| 도메인(내부) | Company, Site, Role, Storage | **HTML POST** | ❌ 없음 |
| 코드 | Code | **HTML POST** | ❌ 없음 |

### 6-2. 상세 설명

#### 업무 모듈 - SPA 방식
화면에서 직접 API 호출:
```html
<form data-form-manager 
      data-action="/api/workorders" 
      data-method="POST" 
      data-redirect="/workorder/detail/{id}">
</form>
```

#### 마스터/도메인 - POST 방식
화면에서 POST로 서버 전송:
```html
<form method="post" th:action="@{/plant/save}">
  <!-- CSRF 토큰 -->
  <input type="hidden" name="_csrf" th:value="${_csrf.token}" />
  <!-- 신규/수정 구분 -->
  <input type="hidden" name="isNew" th:value="${isNew}" />
  <!-- 폼 필드 -->
</form>
```

### 예시

```html
<form data-form-manager 
      data-action="/api/workorders" 
      data-method="POST" 
      data-redirect="/workorder/detail/{id}">
</form>
```

### 신규 Form 객체 생성 규칙 (2025-10-13)

**문제**: Thymeleaf 템플릿에서 `${object.field}` 참조 시 object가 null이면 `SpelEvaluationException` 발생

**해결**: PageController의 `/form` 엔드포인트에서 신규 생성 시 **반드시 빈 객체 생성**

**마스터 모듈** (PlantPageController):
```java
@GetMapping("/plant/form")
public String form(Model model) {
    model.addAttribute("plant", emptyPlant());  // ✅ 빈 객체 생성
    model.addAttribute("isNew", true);
    addReferenceData(model);  // Select box용 참조 데이터
    return "plant/form";
}

private PlantResponse emptyPlant() {
    return new PlantResponse(null, null, ..., "N", ...);
}
```

**도메인 모듈** (DeptController):
```java
@GetMapping("/domain/dept/form")
public String newForm(Model model) {
    model.addAttribute("dept", emptyDept());  // ✅ 빈 객체 생성
    model.addAttribute("isNew", true);
    return "domain/dept/form";
}

private DeptResponse emptyDept() {
    return new DeptResponse(null, null, null, "N", ...);
}
```

**업무 모듈** (2025-10-13 수정):
```java
@GetMapping("/inspection/form")
public String form(@RequestParam(required = false) String id, Model model) {
    InspectionResponse inspection = isNew 
        ? createEmptyInspection(stage)  // ✅ 빈 객체 생성
        : service.get(id);
    model.addAttribute("inspection", inspection);
}

private InspectionResponse createEmptyInspection(String stage) {
    return new InspectionResponse(
        null, ..., 
        "DRAFT",  // status 기본값
        stage != null ? stage : "ACT",  // stage 기본값
        ..., 
        List.of()  // 빈 items
    );
}
```

**적용 모듈**:
- ✅ InspectionPageController (업무)
- ✅ WorkOrderPageController (업무, `stage="PLN"` 기본값)
- ✅ WorkPermitPageController (업무, `stage="PLN"` 고정, Form에서 readonly 처리)
- ✅ MemoPageController (업무)
- ✅ ApprovalPageController (업무)
- ✅ InventoryTxPageController (업무)
- ✅ PlantPageController (마스터, 신규)
- ✅ InventoryPageController (마스터, 신규)

### Form readonly 조건 (2025-10-13)

**원칙**: stage와 status를 조합하여 필드별 수정 가능 여부 제어

**Thymeleaf 표현식** (WorkOrder, Inspection):
```html
<!-- 계획 필드 (plannedDate, plannedCost, plannedLabor) -->
th:readonly="${workOrder.stage != 'PLN' or workOrder.status != 'DRAFT'}"
<!-- → PLN+DRAFT일 때만 수정 가능 -->

<!-- 실적 필드 (actualDate, actualCost, actualLabor) -->
th:readonly="${workOrder.stage != 'ACT' or workOrder.status != 'DRAFT'}"
<!-- → ACT+DRAFT일 때만 수정 가능 -->

<!-- 공통 필드 (name, plantId, memberId 등) -->
th:readonly="${workOrder.status != 'DRAFT'}"
<!-- → status=DRAFT일 때만 수정 가능 (stage 무관) -->
```

**WorkPermit Stage 고정** (2025-10-15):
```html
<!-- Stage 필드는 읽기 전용 (PLN 고정) -->
<input id="stage" name="stage" class="input" value="PLN" readonly />
<small class="help">작업허가는 계획(PLN) 단계만 존재합니다</small>
```
- WorkPermit은 기본적으로 PLN 단계만 사용하지만, 향후 실적 단계(ACT) 확장을 대비해 백엔드 API는 ACT 입력도 수용하도록 설계되어 있습니다.
- Form은 여전히 PLN만 전송하도록 readonly로 고정
- Service는 request.stage를 수용하되 기본값은 PLN (Inspection/WorkOrder와 일관된 구조)
- Form이 readonly이므로 실제로는 항상 PLN만 전송됨

**적용 예시**:

| 상태 | 계획 필드 | 실적 필드 | 공통 필드 |
|------|----------|----------|----------|
| PLN+DRAFT | ✅ 수정 가능 | 🔒 readonly | ✅ 수정 가능 |
| PLN+SUBMT | 🔒 readonly | 🔒 readonly | 🔒 readonly |
| ACT+DRAFT | 🔒 readonly | ✅ 수정 가능 | ✅ 수정 가능 |
| ACT+SUBMT | 🔒 readonly | 🔒 readonly | 🔒 readonly |

**상태 표시 필드**:
```html
<input type="text" readonly
  th:value="${(workOrder.stage == 'PLN' and workOrder.status == 'DRAFT') ? '계획 작성' : 
           (workOrder.stage == 'PLN' and workOrder.status == 'SUBMT') ? '계획 결재상신' : 
           (workOrder.stage == 'PLN' and workOrder.status == 'APPRV') ? '계획 승인완료' : 
           (workOrder.stage == 'ACT' and workOrder.status == 'DRAFT') ? '실적 작성' : 
           (workOrder.stage == 'ACT' and workOrder.status == 'SUBMT') ? '실적 결재상신' : 
           (workOrder.stage == 'ACT' and workOrder.status == 'APPRV') ? '실적 승인완료' : 
           (workOrder.stage ?: '-') + '+' + (workOrder.status ?: '-')}" />
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

**신규 생성 시 초기값** (Service.create):

| 모듈 | 기본 stage | 기본 status | 비고 |
|------|-----------|-------------|------|
| **Inspection** | `ACT` | `DRAFT` | request.stage가 전달되면 우선 사용 (PLN/ACT 모두 가능) |
| **WorkOrder** | `PLN` | `DRAFT` | request.stage가 전달되면 우선 사용 |
| **WorkPermit** | `PLN` | `DRAFT` | **PLN 단계만 존재 (ACT 없음, Form은 readonly로 PLN 고정)** |

**로직 예시**:
```java
// InspectionService.create()
if (request.stage() != null && !request.stage().isBlank()) {
    entity.setStage(request.stage());  // 폼에서 "PLN" 전달 시
} else {
    entity.setStage("ACT");  // 기본값
}
entity.setStatus("DRAFT");

// WorkPermitService.create()
if (request.stage() != null && !request.stage().isBlank()) {
    entity.setStage(request.stage());  // 요청 수용 (확장성)
} else {
    entity.setStage("PLN");  // 기본값 (Form에서 PLN만 전송)
}
entity.setStatus("DRAFT");
```

### 8-2. ApprovalStep 필드

- **decision**: 결재 역할 (`APPRL`, `AGREE`, `INFO`)
- **result**: 결재 결과 (`APPRV`, `REJCT`, `NULL`)
- **ref_entity**: `INSP` / `WORK` / `WPER`
- **ref_id**: 원문서 ID
- **ref_stage**: `PLN` / `ACT`

### 8-3. 결재 아키텍처 (REST + Outbox/Webhook)

Legacy Handler/Facade 패턴을 제거하고, REST API + Outbox/Webhook 조합으로 결재 연계를 단순화했다. 업무 모듈은 Approval API를 통해 상신하고, 결과는 Webhook으로 비동기 통지된다.

#### 8-3-1. 구성 개요

| 계층 | 담당 | 주요 구성 요소 |
|------|------|----------------|
| 결재 Core | ApprovalService | Approval/ApprovalStep 저장, Outbox 이벤트 발행 |
| 전송 | ApprovalOutboxScheduler | `approval_outbox`의 `PENDING` → Webhook POST (HMAC 서명) |
| 수신 | 업무 모듈 Webhook 컨트롤러 | 서명/멱등 검증 후 상태 전이 |
| 모듈 연계 | `*ApprovalService` (Inspection/WorkOrder/WorkPermit) | ApprovalClient로 상신, 상태 SUBMT로 전환 |

#### 8-3-2. ApprovalService + Outbox/Webhook 구조

```java
@Service
@Transactional
public class ApprovalService {
    public ApprovalResponse create(ApprovalRequest request) {
        Approval saved = repository.save(approvalEntity);
        List<ApprovalStep> steps = persistSteps(saved, request.steps());
        enqueueOutbox(saved, steps, ApprovalEventType.SUBMITTED, LocalDateTime.now(), actorId, null);
        return ApprovalResponse.from(saved, toResponses(steps));
    }

    public ApprovalResponse approve(String approvalId, String comment) {
        // 단계 검증 후 승인 처리
        enqueueOutbox(saved, steps, ApprovalEventType.APPROVED, now, actorId, comment);
        return ApprovalResponse.from(saved, toResponses(steps));
    }
}
```

핵심 특징
- Approval/ApprovalStep/Inbox 저장 → Outbox 이벤트 생성까지 하나의 트랜잭션으로 처리
- `approval_outbox` 테이블에 상태(`PENDING`, `SENT`, `FAILED`) 기록, 스케줄러가 Webhook POST
- `approval_webhook_log`, `webhook_idempotency` 로 Webhook 발송/수신 내역 추적
- HMAC 서명(`X-Approval-Signature`)과 멱등키(`X-Approval-Idempotency-Key`)로 보안·중복 방지

#### 8-3-3. 업무 모듈 연계 (ApprovalClient + Webhook)

모듈별 결재 상신 로직은 REST 클라이언트와 Webhook 컨트롤러로 구성한다.

| 구성요소 | 역할 |
|----------|------|
| `ApprovalClient` | `RestTemplate` 기반 `/api/approvals` 호출 |
| `*ApprovalService` | 모듈별 상신 로직, 결재선 DTO → `ApprovalRequest` 변환, 상태 초기화 |
| `*ApprovalWebhookController` | Webhook 수신, 서명 검증 후 `*ApprovalService.applyApprovalStatus` 호출 |
| `WebhookIdempotencyRepository` | 중복 Webhook 차단 |

예시 (Inspection)
```java
ApprovalSubmissionRequest request = ... // UI 모달에서 전달
ApprovalResponse approval = approvalClient.submitApproval(approvalRequest);
inspection.setApprovalId(approval.approvalId());
inspection.setStatus("SUBMT");

@PostMapping("/webhook")
public ResponseEntity<Void> handleWebhook(...) {
    verifySignature(...);
    approvalService.applyApprovalStatus(payload.refId(), payload.refStage(), transition);
}
```

#### 8-3-4. 프런트엔드 상신 UX

| 컴포넌트 | 설명 |
|----------|------|
| `approval-line-modal.html` | 결재선 입력 모달 (공용), 자동완성 제안 영역 포함 |
| `approval-line-modal.js` | 모달 열기/닫기, 결재선 수집, 멤버 자동완성(`GET /api/members/approval-candidates`), REST 호출 |
| `workflow-actions.js` | `submitApproval()` 호출 시 모달 트리거 (Inspection/WorkOrder/WorkPermit) |

버튼 동작 → 모달 → `/api/{module}/{id}/approvals` POST → 성공 알림 → 상세 페이지 갱신.

### 8-4. 결재 흐름

1. UI 모달에서 결재선 입력 후 `/api/{module}/{id}/approvals` 호출  
2. 업무 모듈 `*ApprovalService`가 상태 검증 후 `ApprovalClient.submitApproval()` 실행  
3. ApprovalService가 Outbox 이벤트 생성 및 상태 SUBMT로 저장  
4. 스케줄러가 Webhook 발송 → 업무 모듈 `*ApprovalWebhookController` 수신  
5. 서명/멱등 검증 후 `applyApprovalStatus()` 호출 → 상태 APPRV/REJCT/DRAFT 반영  
6. 실패 시 Outbox 재시도 및 모니터링 API로 확인

### 8-5. 모듈별 결재 연계 표준

#### 업무 ApprovalService

| 모듈 | 상신 메서드 | 상태 전이 메서드 |
|------|-------------|-----------------|
| `InspectionApprovalService` | `submitApproval(inspectionId, ApprovalSubmissionRequest)` | `applyApprovalStatus(id, stage, transition)` |
| `WorkOrderApprovalService` | `submitApproval(workOrderId, ApprovalSubmissionRequest)` | `applyApprovalStatus(id, stage, transition)` |
| `WorkPermitApprovalService` | `submitApproval(permitId, ApprovalSubmissionRequest)` | `applyApprovalStatus(id, stage, transition)` |

#### 업무 Service (상태 전환/단계 준비)

| 모듈 | 상태 전환 메서드 |
|------|-----------------|
| `InspectionService` | `prepareActualStage()`, `onApprovalApprove/Reject/Delete()` |
| `WorkOrderService` | `prepareActualStage()`, `onApprovalApprove/Reject/Delete()` |
| `WorkPermitService` | `onApprovalApprove/Reject/Delete()` (실적 없음) |

#### API 컨트롤러 표준

| Endpoint | 설명 |
|----------|------|
| `POST /api/{module}/{id}/approvals` | 결재 상신 (모달에서 결재선 포함) |
| `POST /api/{module}/{id}/prepare-actual` | 실적 단계 준비(필요 모듈만) |
| `POST /api/{module}/approvals/webhook` | Webhook 수신 (서명 검증 + 멱등) |

---

---

## 9. Service / Repository 표준

### Service 메서드 구성

**도메인 Service (InspectionService / WorkOrderService / WorkPermitService)**

- 기본 CRUD: `list()`, `get()`, `create()`, `update()`, `delete()`
- 결재 결과 반영: `onApprovalApprove()`, `onApprovalReject()`, `onApprovalDelete()`
- 단계 전환/실적 준비: `prepareActualStage()` (필요 모듈만)
- 유틸리티: `applyRequest()`, `resolveId()`, `currentMemberId()`

**모듈 ApprovalService (`InspectionApprovalService` 등)**

- `submitApproval(id, ApprovalSubmissionRequest request)` : 결재선 + 메타 → Approval API 호출
- `applyApprovalStatus(id, stage, ApprovalStatusTransition transition)` : Webhook 수신 후 상태 반영
- 내부 보조: 멱등키/콜백 URL 생성, 결재 본문 구성, 상태 검증

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
    new SeedCodeItem("INVNT", "재고"),
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

### 파일 삭제 정책 (Soft Delete)

**원칙**: 소프트 삭제 (`delete_mark = 'Y'`) + 물리적 파일 원위치 유지

**동작**:
1. **삭제 요청 시**:
   - DB: `file_group.delete_mark = 'Y'`, `file_item.delete_mark = 'Y'` 설정
   - 물리적 파일: `storage/uploads/{companyId}/{fileGroupId}/` 위치에 그대로 유지

2. **조회/다운로드**:
   - `delete_mark = 'N'` 조건으로 필터링하여 활성 파일만 반환

3. **복원**:
   - DB 업데이트만으로 즉시 복원 가능 (`delete_mark = 'N'`)

4. **물리적 삭제** (배치 작업):
   - 월 1회 배치 실행 (매월 1일 새벽 2시)
   - `delete_mark = 'Y'` && `updated_at < 90일 이전` 조건
   - 물리적 파일 삭제 후 DB 레코드도 완전 삭제

**장점**:
- ✅ 실수 복원 가능 (90일 유예 기간)
- ✅ 감사 이력 유지
- ✅ 구현 단순, 안정성 높음
- ✅ 파일 이동 오류 없음

**저장 경로**: `storage/uploads/{companyId}/{fileGroupId}/{fileId}.{ext}`

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

| 항목 | 규칙 | 예시 |
|------|------|------|
| **PageController** | `<Module>PageController` | InspectionPageController, PlantPageController |
| **ApiController** | `<Module>ApiController` | InspectionApiController, PlantApiController |
| **단일 Controller** | `<Module>Controller` | DeptController, CodeController |
| Service | `<Module>Service` | InspectionService, PlantService |
| Module Approval Service | `<Module>ApprovalService` (REST 상신) | InspectionApprovalService |
| Webhook Controller | `<Module>ApprovalWebhookController` | WorkOrderApprovalWebhookController |
| Repository | `<Module>Repository` | InspectionRepository, PlantRepository |
| Entity | PascalCase | Inspection, Plant, Dept |
| JS | kebab-case | inspection.js, plant.js |
| CSS | hyphen-case | base.css, print.css |
| Package | 기능별 colocation | com.cmms11.inspection, com.cmms11.plant |

### Controller 구조 결정 기준

1. **Page + API 분리** (업무 + 마스터):
   - ✅ 복잡한 CRUD 로직
   - ✅ SPA 기능 필요
   - ✅ 외부 API 제공 필요
   - ✅ 파일 크기가 큼 (200줄 이상)

2. **단일 Controller + 선택적 GET API** (도메인 외부 참조):
   - ✅ 단순한 CRUD
   - ✅ Picker/참조용 조회 API만 필요
   - ✅ 화면은 POST 방식
   - ✅ 외부 모듈에서 참조함

3. **단일 Controller + POST만** (도메인 내부 관리):
   - ✅ 단순한 CRUD
   - ✅ 내부 관리만, 외부 참조 없음
   - ✅ 화면은 POST 방식
   - ✅ API 불필요

---

## 17. 향후 계획

- SSO + API Gateway 도입
- NAS → S3 전환
- KPI Dashboard 및 PIMS 연계
- 모바일 UI (2026 상반기)

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
