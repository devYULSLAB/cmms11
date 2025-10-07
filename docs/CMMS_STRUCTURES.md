# CMMS 기술 아키텍처 및 구현 가이드

> **참조 문서**: [CMMS_PRD.md](./CMMS_PRD.md) - 제품 요구사항 및 기능 사양

본 문서는 CMMS 제품 요구사항을 구현하기 위한 기술 아키텍처와 개발 가이드를 정리합니다.
Spring Boot 기반의 레이어드 아키텍처, RBAC 권한 관리, 모바일 대응 UI, 실시간 KPI 대시보드를 핵심으로 합니다.

## 1. 기술 스택 및 아키텍처

### 1.1 기술 스택
- **Backend**: Java 21, Spring Boot 3.3.x
  - Spring Web MVC, Spring Security(RBAC), Spring Data JPA, Bean Validation
  - Spring AOP (권한 체크, 감사 로그)
- **Database**: MariaDB (JDBC 드라이버), Flyway 마이그레이션
- **Build & Tooling**: Gradle, Lombok, MapStruct (DTO 변환)
- **Frontend**: 정적 HTML/CSS/JS (SPA-like 내비게이션)
  - `defaultLayout.html` 기반 SPA 내비게이션 (History API + fetch)
  - 반응형 웹 디자인 (모바일/태블릿 대응)
- **보안**: Spring Security, RBAC, CSRF 방어, XSS 방어

### 1.2 아키텍처 설계 원칙
- **레이어드 아키텍처**: Controller → Service → Repository
- **도메인 중심 설계**: 기능별 패키지 구조 (Feature Colocation)
- **공통 모듈 분리**: 파일 관리, 시퀀스, 권한 등 공통 기능 모듈화
- **표준화된 UI/UX**: Form/List/Detail 표준 레이아웃

## 2. 프로젝트 구조

### 2.1 디렉토리 구조
```
docs/
  CMMS_PRD.md           # 제품 요구사항 정의서
  CMMS_STRUCTURES.md    # 기술 아키텍처 (본 문서)
  CMMS_TABLES.md        # 데이터 모델 설계

src/main/java/com/cmms11/
  Cmms11Application.java
  config/
    SecurityConfig.java      # RBAC 권한 관리, 폼 로그인
    WebConfig.java           # 정적 리소스/템플릿 매핑
    DatabaseConfig.java      # DB 설정, 트랜잭션 관리
  security/
    MemberUserDetailsService.java  # 사용자 인증/권한 매핑
    RoleBasedAccessControl.java    # RBAC 권한 체크 AOP
  web/
    # 도메인별 Controller
    PlantController.java          # 설비 관리
    InventoryController.java      # 재고 관리  
    InspectionController.java     # 예방점검
    WorkOrderController.java      # 작업지시
    WorkPermitController.java     # 작업허가
    ApprovalController.java       # 결재 관리
    BoardController.java          # 게시판
    DashboardController.java      # KPI 대시보드
  domain/
    # 기준정보 도메인
    company/    # 회사 관리
    site/       # 사이트 관리
    dept/       # 부서 관리
    member/     # 사용자 관리
    role/       # 역할 관리
    code/       # 공통코드 관리
  plant/
    # 설비 마스터 관리
    Plant.java, PlantId.java, PlantRepository.java, PlantService.java
  inventory/
    # 재고 마스터 및 수불 관리
    Inventory.java, InventoryHistory.java, InventoryStock.java
  inspection/
    # 예방점검 관리
    Inspection.java, InspectionItem.java
  workorder/
    # 작업지시 관리
    WorkOrder.java, WorkOrderItem.java
  workpermit/
    # 작업허가 관리
    WorkPermit.java, WorkPermitItem.java
  approval/
    # 결재 프로세스
    Approval.java, ApprovalStep.java
  memo/
    # 메모/게시판
    Memo.java
  common/
    seq/        # 자동번호 채번
    file/       # 파일 업로드/다운로드
    audit/      # 감사 로그
    kpi/        # KPI 계산 엔진
    excel/      # 엑셀 I/O 처리

src/main/resources/
  application.yml, application-dev.yml, application-prod.yml
  messages/messages_ko.properties, messages_en.properties  # 국제화
  db/migration/
    V1__baseline.sql      # 초기 스키마
    V2__add_kpi_tables.sql
  templates/
    layout/defaultLayout.html     # SPA 메인 레이아웃
    auth/login.html               # 로그인 페이지
    dashboard/index.html          # KPI 대시보드
    plant/list.html, form.html, detail.html
    inventory/list.html, form.html, detail.html
    inspection/list.html, form.html, detail.html
    workorder/list.html, form.html, detail.html
    workpermit/list.html, form.html, detail.html
    approval/list.html, form.html, detail.html
    board/list.html, form.html, detail.html
  static/assets/
    css/
      base.css           # 기본 스타일 (반응형)
      print.css          # 인쇄용 스타일
    js/
      app.js            # SPA 네비게이션, 공통 UX
      common.js         # 공통 유틸리티
      dashboard.js      # KPI 대시보드
    samples/
      plant-upload-sample.csv
      inventory-upload-sample.csv
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
com.cmms11/
  domain/          # 기준정보 도메인
    company/       # 회사 관리
    site/          # 사이트 관리  
    member/        # 사용자 관리
    role/          # 역할 관리
  plant/           # 설비 마스터
  inventory/       # 재고 마스터
  inspection/      # 예방점검
  workorder/       # 작업지시
  workpermit/      # 작업허가
  approval/        # 결재 프로세스
  memo/            # 메모/게시판
  common/          # 공통 모듈
    seq/           # 자동번호 채번
    file/          # 파일 관리
    kpi/           # KPI 계산
    excel/         # 엑셀 I/O
  web/             # 웹 컨트롤러
  security/        # 보안 관련
  config/          # 설정
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
- **로그인 페이지**: `/auth/login.html`
- **처리 URL**: `/api/auth/login` (Spring Security formLogin)
- **파라미터**: `member_id`(사용자ID), `password`
- **사용자 식별**: 
  - 기본 회사코드: `C0001`
  - 멀티 회사: `회사코드:사용자ID` 형태 (예: `C0002:admin`)
- **성공 시**: `/layout/defaultLayout.html?content=/dashboard/index.html`
- **실패 시**: `/auth/login.html?error=1`
- **로그아웃**: `/api/auth/logout` → `/auth/login.html`

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

#### 4.3.1 defaultLayout 구조
- **파일**: `src/main/resources/templates/layout/defaultLayout.html`
- **동작**: 쿼리스트링 `content` 경로를 fetch로 로딩하여 `#layout-slot`에 삽입
- **History API**: 링크 클릭을 가로채서 경로만 변경, SPA 느낌 제공
- **브라우저 네비게이션**: 뒤/앞으로 가기(popstate) 지원

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
- **전역 위젯**: `static/assets/js/app.js`의 자동 초기화
- **SPA 호환**: `#layout-slot` 주입 환경에서 일관된 동작
- **표준 마크업**: `[data-attachments]` 속성 기반 자동 처리
- **권한 기반 접근**: 업로드자와 승인된 사용자만 접근 가능

#### 4.5.2 표준 마크업 구조
```html
<!-- 편집 가능 화면 -->
<div class="section" data-attachments>
  <input type="hidden" name="fileGroupId" th:value="${entity.fileGroupId}" />
  <div class="attachments">
    <input id="attachments-input" class="visually-hidden" type="file" multiple />
    <button type="button" class="btn" data-attachments-add>파일 선택</button>
    <ul class="attachments-list" aria-live="polite">
      <li class="empty">첨부 파일이 없습니다.</li>
    </ul>
  </div>
</div>

<!-- 읽기 전용 화면 -->
<div class="section" data-attachments data-readonly>
  <input type="hidden" name="fileGroupId" th:value="${entity.fileGroupId}" />
  <ul class="attachments-list" aria-live="polite"></ul>
</div>
```

#### 4.5.3 REST API 엔드포인트
- **업로드**: `POST /api/files` (multipart/form-data, key=`files`)
- **목록 조회**: `GET /api/files?groupId={fileGroupId}`
- **다운로드**: `GET /api/files/{fileId}?groupId={fileGroupId}`
- **삭제**: `DELETE /api/files/{fileId}?groupId={fileGroupId}`

#### 4.5.4 파일 그룹 관리
- **fileGroupId**: 모듈 코드 기반 10자리 고정 길이 (F250119001 형식)
- **저장 정보**: 원본명, 서명, 크기, 해시, 경로
- **보안**: 권한 기반 접근, 서명 URL, 감사 로그

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
// app.js에서 자동 처리
document.querySelector('form[data-validate]').addEventListener('submit', function(e) {
  if (!this.checkValidity()) {
    e.preventDefault();
    // 첫 번째 오류 필드로 포커스 이동
  }
});
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

### 8.1 기술 문서
- **[CMMS_PRD.md](./CMMS_PRD.md)**: 제품 요구사항 정의서
- **[CMMS_TABLES.md](./CMMS_TABLES.md)**: 데이터 모델 설계

### 8.2 관련 문서
- **[PRODUCTION_CHECKLIST.md](./PRODUCTION_CHECKLIST.md)**: 운영 배포 체크리스트
- **[README.md](../README.md)**: 프로젝트 개요 및 설치 가이드