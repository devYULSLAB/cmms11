# CMMS 기술 아키텍처 및 구현 가이드

> **참조 문서**: [CMMS_PRD.md](./CMMS_PRD.md) - 제품 요구사항 및 기능 사양

본 문서는 CMMS 제품 요구사항을 구현하기 위한 기술 아키텍처와 개발 가이드를 정리합니다.
Spring Boot 기반의 레이어드 아키텍처, RBAC 권한 관리, 반응형 웹 UI, 실시간 KPI 대시보드를 핵심으로 합니다.

## 📝 최근 업데이트 (2025-10-08)

**ES 모듈 시스템 및 프로젝트 구조 정리**:
- Frontend: Thymeleaf + ES Modules로 명시
- JavaScript: `main.js` + 분해된 모듈 (`core/`, `api/`, `ui/`)
- Mobile 프로젝트: 별도 관리로 변경 (경로만 간략히 표시)
- Web Controller: Thymeleaf 페이지 전용으로 역할 명확화

## 1. 기술 스택 및 아키텍처

### 1.1 기술 스택
- **Backend**: Java 21, Spring Boot 3.3.x
  - Spring Web MVC, Spring Security(RBAC), Spring Data JPA, Bean Validation
  - Spring AOP (권한 체크, 감사 로그)
- **Database**: MariaDB 10.6+, Flyway 마이그레이션
- **Build & Tooling**: Gradle 8.0+, Lombok
- **Frontend**: Thymeleaf + ES Modules (SPA-like 내비게이션)
  - `defaultLayout.html` 기반 SPA 시스템 (History API + fetch)
  - ES 모듈 시스템 (`main.js` + `core/` + `api/` + `ui/`)
  - 반응형 웹 디자인 (모바일/태블릿 대응)
- **보안**: Spring Security, RBAC, CSRF 방어 (CookieCsrfTokenRepository), XSS 방어

### 1.2 아키텍처 설계 원칙
- **레이어드 아키텍처**: Controller → Service → Repository
- **도메인 중심 설계**: 기능별 패키지 구조 (Feature Colocation)
- **공통 모듈 분리**: 파일 관리, 시퀀스, 권한 등 공통 기능 모듈화
- **표준화된 UI/UX**: Form/List/Detail 표준 레이아웃

## 2. 프로젝트 구조

### 2.1 프로젝트 루트 구조
```
codex/                              # 프로젝트 루트
├── docs/                           # 공통 문서
│   ├── CMMS_PRD.md                # CMMS 제품 요구사항
│   ├── CMMS_STRUCTURES.md         # CMMS 기술 아키텍처 (본 문서)
│   ├── CMMS_TABLES.md             # CMMS 데이터 모델
│   ├── CMMS_JAVASCRIPT.md         # JavaScript 개발 가이드
│   └── CMMS_CSS.md                # CSS 스타일 가이드
│
├── server/                         # 백엔드 서버 프로젝트
│   ├── cmms11/                    # CMMS 백엔드 (Spring Boot) - 본 문서 대상
│   │   ├── src/
│   │   ├── build.gradle
│   │   └── README.md
│   │
│   └── pims11/                    # PIMS 백엔드 (별도 프로젝트)
│       └── ...
│
└── mobile/                         # 모바일 프로젝트 (별도 관리)
    ├── mobile-adapter/            # 모바일 API 어댑터
    └── ...
```

### 2.2 CMMS 백엔드 상세 구조 (server/cmms11/)
```
server/cmms11/
├── src/main/java/com/cmms11/
│   ├── Cmms11Application.java
│   │
│   ├── config/                         # 설정
│   │   ├── SecurityConfig.java         # RBAC 권한 관리, 폼 로그인
│   │   ├── WebConfig.java              # 정적 리소스/템플릿 매핑
│   │   ├── AppConfig.java              # 애플리케이션 설정
│   │   └── RequestLoggingFilter.java   # 요청 로깅
│   │
│   ├── security/                       # 보안
│   │   ├── MemberUserDetailsService.java  # 사용자 인증/권한 매핑
│   │   └── CustomAuthenticationProvider.java
│   │
│   ├── web/                            # 웹 컨트롤러 (Thymeleaf 페이지)
│   │   ├── PlantController.java       # 설비 관리 웹 페이지
│   │   ├── InventoryController.java   # 재고 관리 웹 페이지
│   │   ├── InspectionController.java  # 예방점검 웹 페이지
│   │   ├── WorkOrderController.java   # 작업지시 웹 페이지
│   │   ├── WorkPermitController.java  # 작업허가 웹 페이지
│   │   ├── ApprovalController.java    # 결재 관리 웹 페이지
│   │   ├── MemoController.java        # 메모/게시판 웹 페이지
│   │   ├── AuthController.java        # 인증 웹 페이지
│   │   └── LayoutController.java      # 레이아웃 및 공통 페이지
│   │
│   ├── domain/                         # 기준정보 도메인
│   │   ├── company/                   # 회사 관리
│   │   ├── site/                      # 사이트 관리
│   │   ├── dept/                      # 부서 관리
│   │   ├── member/                    # 사용자 관리
│   │   ├── role/                      # 역할 관리
│   │   └── code/                      # 공통코드 관리
│   │
│   ├── plant/                          # 설비 마스터
│   │   ├── Plant.java, PlantId.java
│   │   ├── PlantRepository.java
│   │   └── PlantService.java
│   │
│   ├── inventory/                      # 재고 마스터
│   │   ├── Inventory.java, InventoryId.java
│   │   ├── InventoryRepository.java
│   │   └── InventoryService.java
│   │
│   ├── inventoryTx/                    # 재고 수불
│   │   ├── InventoryTx.java
│   │   ├── InventoryStock.java
│   │   └── InventoryTxService.java
│   │
│   ├── inspection/                     # 예방점검
│   │   ├── Inspection.java, InspectionId.java
│   │   ├── InspectionItem.java
│   │   ├── InspectionRepository.java
│   │   └── InspectionService.java
│   │
│   ├── workorder/                      # 작업지시
│   │   ├── WorkOrder.java, WorkOrderId.java
│   │   ├── WorkOrderItem.java
│   │   └── WorkOrderService.java
│   │
│   ├── workpermit/                     # 작업허가
│   │   ├── WorkPermit.java, WorkPermitId.java
│   │   ├── WorkPermitItem.java
│   │   └── WorkPermitService.java
│   │
│   ├── approval/                       # 결재 프로세스
│   │   ├── Approval.java, ApprovalId.java
│   │   ├── ApprovalStep.java
│   │   └── ApprovalService.java
│   │
│   ├── memo/                           # 메모/게시판
│   │   ├── Memo.java, MemoId.java
│   │   └── MemoService.java
│   │
│   ├── file/                           # 파일 관리
│   │   ├── FileGroup.java, FileItem.java
│   │   ├── FileService.java
│   │   └── FileController.java
│   │
│   └── common/                         # 공통 모듈
│       ├── seq/                       # 자동번호 채번
│       │   ├── Sequence.java
│       │   └── AutoNumberService.java
│       ├── upload/                    # 파일 업로드
│       ├── error/                     # 예외 처리
│       ├── audit/                     # 감사 로그
│       ├── kpi/                       # KPI 계산 엔진
│       └── excel/                     # 엑셀 I/O 처리
│
├── src/main/resources/
│   ├── application.yml                 # 기본 설정
│   ├── application-dev.yml             # 개발 환경
│   ├── application-prod.yml            # 운영 환경
│   │
│   ├── messages/                       # 국제화
│   │   ├── messages_ko.properties
│   │   └── messages_en.properties
│   │
│   ├── db/migration/                   # Flyway 마이그레이션
│   │   ├── V1__baseline.sql
│   │   └── V2__add_kpi_tables.sql
│   │
│   ├── templates/                      # Thymeleaf 템플릿
│   │   ├── layout/
│   │   │   └── defaultLayout.html     # SPA 메인 레이아웃
│   │   ├── auth/
│   │   │   └── login.html             # 로그인 페이지
│   │   ├── plant/
│   │   │   ├── list.html, form.html, detail.html
│   │   │   └── history.html
│   │   ├── inventory/
│   │   │   ├── list.html, form.html, detail.html
│   │   │   └── uploadForm.html
│   │   ├── inventoryTx/
│   │   │   ├── transaction.html
│   │   │   ├── ledger.html
│   │   │   └── closing.html
│   │   ├── inspection/
│   │   │   ├── list.html, form.html, detail.html
│   │   │   └── plan.html
│   │   ├── workorder/
│   │   │   └── list.html, form.html, detail.html
│   │   ├── workpermit/
│   │   │   └── list.html, form.html, detail.html
│   │   ├── approval/
│   │   │   └── list.html, form.html, detail.html
│   │   ├── memo/
│   │   │   └── list.html, form.html, detail.html
│   │   ├── domain/                    # 기준정보 화면
│   │   │   ├── company/, site/, dept/
│   │   │   ├── member/, role/
│   │   │   └── func/, storage/
│   │   └── common/                    # 공통 화면
│   │       ├── profile-edit.html
│   │       ├── plant-picker.html
│   │       └── org-picker.html
│   │
│   └── static/                         # 정적 리소스
│       ├── assets/
│       │   ├── css/
│       │   │   ├── base.css           # 기본 스타일 (반응형)
│       │   │   ├── print.css          # 인쇄용 스타일
│       │   │   └── layout.css         # 레이아웃 스타일
│       │   ├── js/
│       │   │   ├── main.js            # ES 모듈 엔트리 포인트
│       │   │   ├── core/              # 핵심 시스템
│       │   │   │   ├── index.js
│       │   │   │   ├── csrf.js
│       │   │   │   ├── navigation.js
│       │   │   │   ├── module-loader.js
│       │   │   │   ├── pages.js
│       │   │   │   └── utils.js
│       │   │   ├── api/               # API 계층
│       │   │   │   ├── index.js
│       │   │   │   ├── auth.js
│       │   │   │   └── storage.js
│       │   │   ├── ui/                # UI 컴포넌트
│       │   │   │   ├── index.js
│       │   │   │   ├── notification.js
│       │   │   │   ├── file-upload.js
│       │   │   │   ├── file-list.js
│       │   │   │   ├── table-manager.js
│       │   │   │   ├── data-loader.js
│       │   │   │   ├── confirm-dialog.js
│       │   │   │   ├── validator.js
│       │   │   │   └── print-utils.js
│       │   │   └── pages/             # 페이지별 모듈
│       │   │       ├── plant.js
│       │   │       ├── inventory.js
│       │   │       ├── inspection.js
│       │   │       ├── workorder.js
│       │   │       ├── workpermit.js
│       │   │       ├── approval.js
│       │   │       ├── memo.js
│       │   │       ├── member.js
│       │   │       ├── code.js
│       │   │       └── domain.js
│       │   └── samples/
│       │       ├── plant-upload-sample.csv
│       │       └── inventory-upload-sample.csv
│       └── favicon.ico
│
├── src/test/java/com/cmms11/           # 테스트
│   ├── integration/                   # 통합 테스트
│   └── unit/                          # 단위 테스트
│
├── build.gradle                        # Gradle 빌드 설정
├── settings.gradle                     # Gradle 설정
├── gradle.properties
├── gradlew, gradlew.bat
│
├── scripts/                            # 실행 스크립트
│   ├── start-dev.sh, start-dev.bat    # 개발 실행
│   ├── start-prod.sh                  # 운영 실행
│   └── stop-dev.sh, stop-prod.sh      # 종료
│
├── storage/                            # 파일 저장소 (로컬)
│   └── uploads/
│
└── logs/                               # 로그 파일
```

## 3. 레이어드 아키텍처 및 네이밍 규칙

### 3.1 레이어 구성
- **Controller**: HTTP 매핑, 요청/응답 처리, 권한 체크, DTO 변환
- **Service**: 트랜잭션 경계, 비즈니스 로직, 도메인 규칙, Repository 조합
- **Repository**: 엔티티 영속화, 복잡한 조회 쿼리, 데이터 접근
- **Entity**: 도메인 모델, JPA 매핑, 비즈니스 규칙 캡슐화

**중요 규칙**: Controller는 절대 Repository를 직접 호출하지 않음. 반드시 Service를 통해 접근

### 3.2 패키지 구조 (Feature Colocation)
```
server/cmms11/src/main/java/com/cmms11/
  ├── config/              # 설정
  │   ├── SecurityConfig
  │   ├── WebConfig
  │   └── AppConfig
  │
  ├── security/            # 보안
  │   ├── MemberUserDetailsService
  │   └── CustomAuthenticationProvider
  │
  ├── web/                 # 웹 페이지 컨트롤러 (Thymeleaf)
  │   ├── PlantController
  │   ├── InventoryController
  │   ├── InspectionController
  │   ├── WorkOrderController
  │   ├── WorkPermitController
  │   ├── ApprovalController
  │   ├── MemoController
  │   ├── AuthController
  │   └── LayoutController
  │
  ├── domain/             # 기준정보 도메인
  │   ├── company/       # 회사 관리
  │   ├── site/          # 사이트 관리
  │   ├── dept/          # 부서 관리
  │   ├── member/        # 사용자 관리
  │   ├── role/          # 역할 관리
  │   └── code/          # 공통코드 관리
  │
  ├── plant/             # 설비 마스터
  ├── inventory/         # 재고 마스터
  ├── inventoryTx/       # 재고 수불
  ├── inspection/        # 예방점검
  ├── workorder/         # 작업지시
  ├── workpermit/        # 작업허가
  ├── approval/          # 결재 프로세스
  ├── memo/              # 메모/게시판
  ├── file/              # 파일 관리
  │
  └── common/            # 공통 모듈
      ├── seq/          # 자동번호 채번
      ├── upload/       # 파일 업로드
      ├── error/        # 예외 처리
      ├── audit/        # 감사 로그
      ├── kpi/          # KPI 계산
      └── excel/        # 엑셀 I/O
```

### 3.3 네이밍 규칙

#### 3.3.1 Java 클래스 네이밍
- **Entity**: PascalCase (예: `Plant`, `Inspection`, `WorkOrder`)
- **Embedded ID**: `<Entity>Id` (예: `PlantId`, `InspectionId`)
- **Repository**: `<Entity>Repository` (예: `PlantRepository`)
- **Service**: `<Entity>Service` (예: `PlantService`)
- **Controller**: `<Entity>Controller` (예: `PlantController`)
- **DTO**: `<Entity>Request`, `<Entity>Response` (예: `PlantRequest`, `PlantResponse`)

#### 3.3.2 메서드 네이밍 컨벤션

**Service 계층**:
- **Query**: `get*(id)` (없으면 예외), `list*(pageable)`, `search*(keyword, pageable)`
- **Command**: `create*(request)`, `update*(id, request)`, `delete*(id)`
- **비즈니스 로직**: `confirm*(id)`, `approve*(id)`, `reject*(id)`

**Controller 계층**:
- **API**: `list*`, `get*`, `create*`, `update*`, `delete*`
- **Web Form**: `listForm`, `getForm`, `newForm`, `editForm`

**Repository 계층**:
- Spring Data JPA 규칙: `findBy*`, `existsBy*`, `countBy*`
- 커스텀 쿼리: `@Query` 사용

### 3.4 RBAC 권한 관리 시스템

#### 3.4.1 권한 구조 설계
CMMS 시스템은 **사용자-역할(1:1)-허가(1:N)** 구조의 RBAC 모델을 사용합니다.

**핵심 개념**:
- **사용자(Member)**: 시스템을 사용하는 개인
- **역할(Role)**: 업무 책임과 권한을 정의하는 그룹 (예: 관리자, 기술자, 조회자)
- **허가(Permission)**: 구체적인 시스템 기능에 대한 접근 권한 (예: PLANT_C, INSPECTION_R)

#### 3.4.2 허가 명명 규칙
허가는 **`[모듈명]_[CRUD]`** 형식으로 명명됩니다:
- 모듈명: 대문자 영문 (예: PLANT, INSPECTION, WORKORDER)
- CRUD: C(생성), R(조회), U(수정), D(삭제)
- 예시: `PLANT_C`, `INSPECTION_R`, `WORKORDER_U`

#### 3.4.3 표준 역할 정의

**ADMIN (관리자)**:
- 모든 모듈의 모든 CRUD 권한

**MANAGER (담당자)**:
- 담당 업무 모듈의 모든 CRUD 권한
- 관련 모듈의 조회 권한

**TECHNICIAN (기술자)**:
- 담당 업무의 생성, 조회, 수정 권한
- 관련 모듈의 조회 권한

**VIEWER (조회자)**:
- 모든 모듈의 조회 권한만

#### 3.4.5 모듈별 권한 체크 표준

**Controller에서 권한 체크**: 모든 Controller 메서드에 `@PreAuthorize` 적용

**기준정보 모듈**: `COMPANY_C/R/U/D`, `SITE_C/R/U/D`, `DEPT_C/R/U/D`, `MEMBER_C/R/U/D`, `ROLE_C/R/U/D`, `CODE_C/R/U/D`

**마스터 모듈**: `PLANT_C/R/U/D`, `INVENTORY_C/R/U/D`

**트랜잭션 모듈**: `INSPECTION_C/R/U/D`, `WORKORDER_C/R/U/D`, `WORKPERMIT_C/R/U/D`, `APPROVAL_C/R/U/D`

**시스템 모듈**: `DASHBOARD_R`, `FILE_C/R/D`, `REPORT_R`

#### 3.4.6 권한 체크 패턴

**기본 CRUD**: `@PreAuthorize("hasAuthority('MODULE_C/R/U/D')")`

#### 3.4.9 특별 권한 규칙 및 고려사항

**업무 프로세스별 권한 제어**:
- **점검 확정 후 수정**: 점검 완료 상태에서는 결과 수정 불가
- **작업지시 승인 후 변경**: 승인된 작업지시는 담당자 변경 시 재승인 필요
- **결재 진행 중 수정**: 결재 진행 중인 문서는 작성자만 수정 가능

**조직 계층별 접근 제어**:
- **부서별 데이터 접근**: 사용자 소속 부서와 동일한 부서 데이터만 접근
- **사이트별 데이터 접근**: 사용자 기본 사이트와 동일한 사이트 데이터만 접근
- **계층적 접근**: 상위 부서는 하위 부서 데이터 접근 가능 (설정에 따라)

**업무 단계별 권한 제어**:
- **계획 단계**: 모든 권한 사용자만 계획 수립 가능
- **진행 단계**: 담당자와 승인자만 수정 가능
- **완료 단계**: 관리자만 결과 수정 가능

**데이터 보안 고려사항**:
- **민감 정보 접근**: 개인정보, 설비 상세 정보는 특별 권한 필요
- **외부 접근 제한**: 외부 업체 사용자는 제한된 모듈만 접근
- **임시 권한**: 특정 프로젝트나 작업에 대한 임시 권한 부여 기능

## 4. 핵심 기능 구현

### 4.1 인증 및 권한 관리

#### 4.1.1 로그인 시스템

**로그인 페이지 처리 흐름**:
1. **페이지 로드**: `/auth/login.html`
   - ES 모듈 방식으로 최소한의 JavaScript만 로드 (`core/csrf.js`, `ui/validator.js`)
   - HTML5 폼 검증 활성화 (`data-validate` 속성)
2. **폼 제출**: `POST /api/auth/login`
   - 파라미터: `member_id`(사용자ID), `password`, `_csrf`(CSRF 토큰)
3. **Spring Security 처리**: `SecurityConfig.filterChain()`
   - CSRF 토큰 검증 (`CookieCsrfTokenRepository`)
   - 사용자 인증 (`MemberUserDetailsService.loadUserByUsername()`)
   - 비밀번호 검증 (`BCryptPasswordEncoder`)
4. **성공 처리**: 
   - 리다이렉트: `/layout/defaultLayout.html?content=/memo/list`
   - 세션 생성 및 CSRF 쿠키 설정
5. **실패 처리**: 
   - 리다이렉트: `/auth/login.html?error=1`
   - 에러 메시지 표시

**사용자 식별 규칙**: 
- 기본 회사코드: `C0001`
- 멀티 회사: `회사코드:사용자ID` 형태 (예: `C0002:admin`)

**로그아웃 처리**:
- URL: `/api/auth/logout` → `/auth/login.html`
- 세션 무효화 및 쿠키 삭제
- JSON 응답으로 SPA 호환

#### 4.1.2 RBAC 권한 관리
```java
@Service
public class MemberUserDetailsService implements UserDetailsService {
    // 사용자 권한 매핑
    // ADMIN: 모든 권한
    // MANAGER: 트랜잭션 CRUD + 기준정보 조회
    // TECHNICIAN: 점검/작업 미확정 전까지 CRUD
    // VIEWER: 모든 모듈 Read만
}
```

### 4.2 KPI 대시보드 구현

#### 4.2.1 실시간 KPI 계산
```java
@Service
public class KpiCalculationService {
    // 설비 가동률: PLANT + WORKORDER 테이블 기반
    public BigDecimal calculateEquipmentAvailability(String companyId, String siteId);
    
    // 예방점검 준수율: INSPECTION + INSPECTION_RESULT 기반
    public BigDecimal calculateInspectionCompliance(String companyId, LocalDate from, LocalDate to);
    
    // MTTR/MTBF: WORKORDER 작업 시간 기반
    public BigDecimal calculateMTTR(String companyId, LocalDate from, LocalDate to);
    public BigDecimal calculateMTBF(String companyId, String plantId);
    
    // 재고 회전율: INVENTORY_HISTORY 기반
    public BigDecimal calculateInventoryTurnover(String companyId, String storageId, int year);
}
```

#### 4.2.2 알림 시스템
```java
@Component
public class KpiAlertService {
    // 임계값 기반 알림
    @Scheduled(cron = "0 0 9 * * *") // 매일 오전 9시
    public void checkKpiThresholds();
    
    // 트렌드 경고
    public void checkTrendWarnings();
    
    // 예외 상황 알림
    public void sendEmergencyAlerts();
}
``` 

### 4.3 SPA 내비게이션 시스템

#### 4.3.1 defaultLayout 구조 및 초기화 과정

**파일**: `src/main/resources/templates/layout/defaultLayout.html`

**인라인 스크립트의 역할과 동작 순서**:

1. **서버 사이드 데이터 주입** (Thymeleaf `th:inline="javascript"`):
   ```javascript
   // 초기 콘텐츠 설정 (서버에서 전달)
   window.initialContent = '/memo/list';  // Thymeleaf로 동적 설정
   
   // 파일 업로드 설정 (서버 설정값 전달)
   window.fileUploadConfig = {
       maxSize: 10485760,
       allowedExtensions: ['jpg', 'jpeg', 'png', 'pdf', ...],
       maxSizeFormatted: '10MB',
       profile: 'default'
   };
   ```
   **이유**: 서버 설정값을 클라이언트로 전달하기 위함. 환경별(dev/prod) 설정 차이를 반영.

2. **프로필 편집 팝업 처리** (즉시 실행 함수):
   ```javascript
   (function() {
       const profileButton = document.getElementById("btn-profile-edit");
       profileButton.addEventListener("click", () => {
           // 팝업 창 열기
           window.open("/common/profile-edit", ...);
       });
       
       // 팝업에서 메시지 수신 (postMessage API)
       window.addEventListener("message", (event) => {
           if (event.data.type === "PROFILE_UPDATED") {
               // 헤더의 사용자 정보 업데이트
               document.querySelector(".user-detail").textContent = ...;
           }
       });
   })();
   ```
   **이유**: 팝업-부모 창 간 통신을 위한 이벤트 리스너를 즉시 등록. ES 모듈 로드 전에 동작해야 함.

**SPA 동작 방식**:
- **초기 로드**: `window.initialContent`로 지정된 페이지를 `#layout-slot`에 삽입
- **네비게이션**: `core/navigation.js`의 `navigate()` 함수로 새 콘텐츠 동적 삽입
- **History API**: `pushState()`로 URL 변경, `popstate` 이벤트로 뒤/앞으로 가기 지원
- **모듈 로딩**: URL 기반으로 필요한 페이지 모듈(`pages/*.js`) 동적 로드

#### 4.3.2 신규 화면 추가 규칙
- `templates/<feature>/<view>.html`에 화면 정의
- 레이아웃에서 `?content=/<feature>/<view>.html`로 연결
- 표준 화면: `list.html`, `form.html`, `detail.html`

### 4.4 자동번호 채번 시스템

#### 4.4.1 구현 구조
- **구현 클래스**: `common.seq.AutoNumberService`
- **동시성 제어**: `Sequence` 테이블 비관적 쓰기 락 (`findForUpdate`)
- **트랜잭션**: Service 계층에서 `@Transactional` 적용

#### 4.4.2 번호 생성 규칙

**Master ID (기준정보)**:
- 형식: `{moduleCode(1)}{000000}{3자리시퀀스}`
  - API: `generateMasterId(companyId, moduleCode)`
- 모듈코드 매핑:
  - Plant(설비): `1` → 1000000001
  - Inventory(재고): `2` → 2000000001

**Transaction ID (업무데이터)**:
- 형식: `{moduleCode(1)}{YYMMDD}{3자리시퀀스}`
  - API: `generateTxId(companyId, moduleCode, date)`
- 모듈코드 매핑:
  - Inspection(점검): `I` → I250119001
  - WorkOrder(작업지시): `O` → O250119001
  - WorkPermit(작업허가): `P` → P250119001
  - Approval(결재): `A` → A250119001
  - File(파일): `F` → F250119001
  - Memo(메모): `M` → M250119001

#### 4.4.3 동작 방식
- **초기 생성**: 시퀀스 테이블에 (companyId, moduleCode, dateKey) 행이 없으면 신규 생성
- **증분 처리**: `next_seq`에서 현재값 반환 후 +1 저장
- **월별 리셋**: Transaction ID는 월별로 시퀀스 리셋 (최대 999건/월)

### 4.5 파일 관리 시스템

#### 4.5.1 파일 업로드 아키텍처

**모듈 구조**:
- **UI 모듈**: `ui/file-upload.js` - 파일 선택, 표시, 검증
- **Navigation**: `core/navigation.js` - Form submit 시 자동 업로드
- **서버**: `FileController`, `FileService` - 파일 저장 및 관리

**자동 업로드 메커니즘**:
1. 사용자가 Form에서 파일 선택 (UI)
2. Submit 버튼 클릭
3. `navigation.js`가 자동으로 파일 감지
4. Form submit 전에 `/api/files`로 파일 업로드
5. 응답으로 받은 `fileGroupId`를 hidden field에 설정
6. Form 데이터 전송 (fileGroupId 포함)
7. 서버에서 fileGroupId로 파일 연결

**특징**:
- ✅ **자동화**: Form에 `[data-file-upload]` 속성만 추가하면 자동 동작
- ✅ **SPA 호환**: 동적 콘텐츠 로드 환경에서 일관된 동작
- ✅ **선택적**: 파일이 없어도 Form 정상 동작
- ✅ **권한 기반 접근**: 업로드자와 승인된 사용자만 접근 가능

#### 4.5.2 표준 마크업 구조

```html
<!-- Form 페이지 (편집 가능) -->
<form data-validate method="post" th:action="@{/memo/save}">
  <!-- 기본 필드들 -->
  <input type="hidden" name="fileGroupId" th:value="${memo.fileGroupId}" />
  
  <!-- 파일 업로드 영역 -->
  <div class="section" data-file-upload>
    <div class="section-title">첨부</div>
    <div class="attachments">
      <input id="attachments-input" class="visually-hidden" type="file" 
             multiple accept=".jpg,.png,.pdf,.doc,.xls,.hwp,.zip" />
      <button type="button" class="btn" data-attachments-add>
        파일 선택
        <small class="hint">(최대 10MB)</small>
      </button>
      <ul class="file-list" aria-live="polite">
        <li class="empty">첨부된 파일이 없습니다.</li>
      </ul>
    </div>
  </div>
  
  <button type="submit" class="btn primary">저장</button>
</form>

<!-- Detail 페이지 (읽기 전용) -->
<div class="section">
  <div class="section-title">첨부</div>
  <div data-file-list 
       th:if="${memo.fileGroupId}"
       th:attr="data-file-group-id=${memo.fileGroupId}">
    <div class="file-list"></div>
  </div>
</div>
```

#### 4.5.3 JavaScript API

**ui/file-upload.js**:
```javascript
// Form에서 파일 자동 추출 및 업로드
const fileGroupId = await window.cmms.fileUpload.uploadFormFiles(form);
// 반환: "F250107001" 또는 null (파일 없으면)

// 저수준 API 호출 (내부용)
const fileGroupId = await window.cmms.fileUpload.uploadToServer(formData);
```

**core/navigation.js**:
- `data-validate` form의 submit 이벤트에 자동으로 파일 업로드 적용
- 유효성 검사 → 파일 업로드 → Form submit 순서로 실행

#### 4.5.4 REST API 엔드포인트

- **업로드**: `POST /api/files` 
  - Content-Type: `multipart/form-data`
  - 파라미터: `files` (MultipartFile[]), `refEntity` (선택), `refId` (선택)
  - 응답: `{ fileGroupId: "F250107001", items: [...] }`
  
- **목록 조회**: `GET /api/files?groupId={fileGroupId}`
  - 응답: `{ fileGroupId, items: [{ id, originalName, size, ... }] }`
  
- **다운로드**: `GET /api/files/{fileId}?groupId={fileGroupId}`
  - 응답: 파일 바이너리 (Content-Disposition: attachment)
  
- **삭제**: `DELETE /api/files/{fileId}?groupId={fileGroupId}`
  - 응답: 204 No Content

#### 4.5.5 파일 그룹 관리

**fileGroupId 생성**:
- 형식: `F` + `YYMMDD` + `순번(3자리)` = 10자리
- 예시: `F250107001` (2025년 1월 7일 첫 번째 파일 그룹)
- 생성 시점: 파일 업로드 API 호출 시 (`POST /api/files`)
- 조건: 파일이 1개 이상 있을 때만 생성

**저장 정보**:
- `file_group`: fileGroupId, refEntity, refId, 생성일시, 생성자
- `file_item`: fileId, 원본명, 저장명, 확장자, MIME, 크기, 해시(SHA256), 저장 경로

**보안**:
- 권한 기반 접근 제어
- 경로 traversal 방지 (normalize, startsWith 검증)
- 파일 크기 제한 (기본 10MB)
- 허용 확장자 검증
- 업로드 시 SHA256 체크섬 계산

### 4.6 엑셀 I/O 시스템

#### 4.6.1 대량 데이터 업로드
- **엔드포인트**: `POST /api/{module}/upload` (multipart/form-data)
- **응답 형식**: `BulkUploadResult` (성공/실패 건수 + 오류 목록)
- **검증**: 컬럼 매핑, 데이터 유효성, 비즈니스 규칙 체크

#### 4.6.2 CSV 템플릿
- **설비 업로드**: `plant_id(선택)`, `name`, `asset_id`, `site_id`, `dept_id`, `func_id`, `install_date`
- **재고 업로드**: `inventory_id(선택)`, `name`, `asset_id`, `dept_id`, `maker_name`, `spec`, `model`, `serial`
- **샘플 파일**: `static/assets/samples/` 디렉토리 제공

## 5. UI/UX 디자인 가이드

### 5.1 반응형 레이아웃 시스템

#### 5.1.1 페이지 구조
```html
<!-- SPA 레이아웃 (defaultLayout.html) -->
<header class="appbar">
  <div class="appbar-inner">
    <!-- 앱바 내용 -->
  </div>
</header>
<nav class="breadcrumbs">
  <span class="sep">/</span>
</nav>
<main>
  <div class="container">
    <!-- 콘텐츠 슬롯 (#layout-slot) -->
  </div>
</main>
<footer>
  <div class="container">© ...</div>
</footer>
```

#### 5.1.2 카드/섹션 구성
```html
<section class="card">
  <div class="card-header">
    <h2 class="card-title">제목</h2>
    <div class="toolbar">
      <!-- 액션 버튼들 -->
    </div>
  </div>
  <div class="card-body">
    <div class="section">
      <h3 class="section-title">섹션 제목</h3>
      <!-- 섹션 내용 -->
    </div>
  </div>
</section>
```

### 5.2 반응형 그리드 시스템

#### 5.2.1 12열 그리드
```html
<div class="grid cols-12">
  <div class="col-span-6">6열</div>
  <div class="col-span-6">6열</div>
</div>
```

#### 5.2.2 스택 레이아웃 (모바일 대응)
```html
<!-- 읽기 전용 -->
<div class="stack">
  <div class="stack-item">
    <label>라벨</label>
    <span>값</span>
  </div>
</div>

<!-- 폼 입력 -->
<div class="form-row">
  <label class="label required" for="field">필드명</label>
  <input class="input" id="field" type="text" required>
</div>
```

### 5.3 표준 컴포넌트

#### 5.3.1 테이블 패턴
```html
<table class="table">
  <thead>
    <tr>
      <th>컬럼1</th>
      <th>컬럼2</th>
      <th>액션</th>
    </tr>
  </thead>
  <tbody>
    <tr data-row-link="detail.html">
      <td><a href="detail.html">값1</a></td>
      <td>값2</td>
      <td>
        <button class="btn btn-sm">수정</button>
        <button class="btn btn-sm btn-danger" data-confirm>삭제</button>
      </td>
    </tr>
  </tbody>
</table>
```

#### 5.3.2 버튼/배지 스타일
```html
<!-- 버튼 -->
<button class="btn">기본</button>
<button class="btn btn-primary">주요</button>
<button class="btn btn-sm">소형</button>
<button class="btn btn-danger" data-confirm="정말 삭제하시겠습니까?">위험</button>

<!-- 배지 -->
<span class="badge">PLAN</span>
<span class="badge badge-warning">PROC</span>
<span class="badge badge-success">DONE</span>
```

### 5.4 폼 검증 시스템

#### 5.4.1 HTML5 검증
```html
<form data-validate>
  <div class="form-row">
    <label class="label required" for="name">이름</label>
    <input class="input" id="name" type="text" required maxlength="50">
  </div>
  <div class="form-row">
    <label class="label required" for="email">이메일</label>
    <input class="input" id="email" type="email" required>
  </div>
  <button type="submit" class="btn btn-primary">저장</button>
</form>
```

#### 5.4.2 커스텀 검증
```javascript
// ui/validator.js에서 자동 처리
export function initValidator() {
  document.addEventListener('submit', (e) => {
    const form = e.target.closest('form[data-validate]');
    if (!form) return;
    
    if (!form.checkValidity()) {
      e.preventDefault();
      // 첫 번째 오류 필드로 포커스 이동
      const firstInvalid = form.querySelector(':invalid');
      if (firstInvalid) {
        firstInvalid.focus();
      }
    }
  }, { capture: true });
}
```

### 5.5 모바일 최적화

#### 5.5.1 반응형 브레이크포인트
- **데스크톱**: 1024px 이상
- **태블릿**: 768px ~ 1023px
- **모바일**: 767px 이하

#### 5.5.2 터치 친화적 UI
- 최소 터치 영역: 44px × 44px
- 그리드 → 스택 레이아웃 자동 전환
- 터치 제스처 지원 (스와이프, 핀치)


## 6. 메뉴 구조 및 화면 구성

### 6.1 메뉴 트리 구조
```
CMMS 메인 메뉴
├── 대시보드 (/dashboard)
├── 기준정보 관리
│   ├── 회사 관리 (/domain/company)
│   ├── 사이트 관리 (/domain/site)
│   ├── 부서 관리 (/domain/dept)
│   ├── 사용자 관리 (/domain/member)
│   ├── 역할 관리 (/domain/role)
│   └── 공통코드 (/domain/code)
├── 설비 관리 (/plant)
├── 재고 관리 (/inventory)
├── 예방점검 (/inspection)
├── 작업 관리
│   ├── 작업지시 (/workorder)
│   └── 작업허가 (/workpermit)
├── 결재 관리 (/approval)
├── 게시판 (/board)
└── 시스템 관리
    ├── 파일 관리 (/files)
    └── 보고서 (/reports)
```

### 6.2 표준 화면 구성

#### 6.2.1 화면 유형별 특징
- **list.html**: 목록 조회 (검색, 페이징, 신규등록 버튼, 액션 컬럼)
- **form.html**: 등록/수정 폼 (ID 자동 생성, 검증, 파일 첨부)
- **detail.html**: 상세 조회 (읽기 전용, 수정/삭제 버튼, 인쇄 기능)

#### 6.2.2 도메인별 화면 제공
- **기준정보 도메인**: form.html, list.html (상세조회 불필요)
- **마스터 도메인**: 모든 화면 제공 (설비, 재고)
- **트랜잭션 도메인**: 모든 화면 제공 (점검, 작업, 결재)

### 6.3 상태 관리 시스템

#### 6.3.1 점검(Inspection) 상태
```java
public enum InspectionStatus {
    PLAN("계획"),     // 점검 계획 수립
    PROC("진행"),     // 점검 진행 중
    DONE("완료")      // 점검 완료
}
```

#### 6.3.2 작업지시(WorkOrder) 상태
```java
public enum WorkOrderStatus {
    PLAN("계획"),     // 작업 계획 수립
    ASGN("배정"),     // 담당자 배정
    PROC("진행"),     // 작업 진행 중
    DONE("완료")      // 작업 완료
}
```

#### 6.3.3 작업허가(WorkPermit) 상태
```java
public enum WorkPermitStatus {
    PLAN("계획"),     // 작업허가 계획 수립
    ASGN("배정"),     // 담당자 배정
    PROC("진행"),     // 작업허가 진행 중
    DONE("완료")      // 작업허가 완료
}
```

#### 6.3.4 UI 배지 표시
```html
<!-- 상태별 배지 색상 -->
<span class="badge">PLAN</span>                    <!-- 기본 (회색) -->
<span class="badge badge-warning">ASGN</span>      <!-- 경고 (노란색) -->
<span class="badge badge-warning">PROC</span>      <!-- 경고 (주황색) -->
<span class="badge badge-success">DONE</span>      <!-- 성공 (초록색) -->
``` 

## 7. 개발 및 배포 가이드

### 7.1 개발 환경 설정

#### 7.1.1 필수 요구사항
- **Java**: JDK 21 이상
- **IDE**: IntelliJ IDEA 또는 Eclipse (Spring Boot 지원)
- **Database**: MariaDB 10.6 이상
- **Build Tool**: Gradle 8.0 이상

#### 7.1.2 개발 환경 구성
```bash
# 프로젝트 클론
git clone [repository-url]
cd cmms11

# 의존성 설치
./gradlew build

# 개발 서버 실행
./gradlew bootRun

# 또는 IDE에서 Cmms11Application.java 실행
```

### 7.2 빌드 및 배포

#### 7.2.1 빌드 설정
```bash
# 개발 빌드
./gradlew bootJar

# 프로덕션 빌드
./gradlew bootJar -Pprofile=prod

# Docker 이미지 빌드 (선택사항)
docker build -t cmms11:latest .
```

#### 7.2.2 프로파일별 설정
- **개발**: `application-dev.yml` (H2 인메모리 DB, 디버그 로그)
- **테스트**: `application-test.yml` (테스트 DB, 제한된 로그)
- **운영**: `application-prod.yml` (MariaDB, 최적화된 로그)

### 7.3 성능 최적화

#### 7.3.1 데이터베이스 최적화
```sql
-- 인덱스 생성 (성능 향상)
CREATE INDEX idx_workorder_status ON work_order(status);
CREATE INDEX idx_inspection_date ON inspection(planned_date);
CREATE INDEX idx_inventory_history ON inventory_history(transaction_date);

-- 쿼리 최적화
-- 페이징 쿼리 사용
-- N+1 문제 방지 (fetch join 사용)
```

#### 7.3.2 애플리케이션 최적화
```java
// JPA 최적화
@Query("SELECT p FROM Plant p JOIN FETCH p.site WHERE p.companyId = :companyId")
List<Plant> findAllWithSite(@Param("companyId") String companyId);

// 캐싱 적용 (선택사항)
@Cacheable("plantCache")
public Plant getPlant(String plantId) { ... }
```

### 7.4 모니터링 및 로깅

#### 7.4.1 로깅 설정
```yaml
# application-prod.yml
logging:
  level:
    com.cmms11: INFO
    org.springframework.security: WARN
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/cmms11.log
    max-size: 100MB
    max-history: 30
```

#### 7.4.2 헬스 체크
```java
@RestController
public class HealthController {
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(status);
    }
}
```

### 7.5 보안 설정

#### 7.5.1 HTTPS 설정 (운영환경)
```yaml
# application-prod.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
  port: 443
```

#### 7.5.2 보안 헤더 설정
```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                )
            )
            .build();
    }
}
```

---

## 8. 참조 문서

### 8.1 관련 문서
- **[CMMS_PRD.md](./CMMS_PRD.md)**: 제품 요구사항 정의서
- **[CMMS_TABLES.md](./CMMS_TABLES.md)**: 데이터 모델 설계
- **[CMMS_JAVASCRIPT.md](./CMMS_JAVASCRIPT.md)**: JavaScript 개발 가이드 (ES 모듈)
- **[CMMS_CSS.md](./CMMS_CSS.md)**: CSS 스타일 가이드
- **[README.md](../README.md)**: 프로젝트 개요 및 시작 가이드

### 8.2 외부 참조
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security](https://docs.spring.io/spring-security/reference/)
- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
- [ES Modules (MDN)](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Modules)