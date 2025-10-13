1. 목표 
  - 표준화되고 재활용성이 높은 코드를 통해, 안정적이고 유지보수 효율을 유지
  - 웹 컨트롤로러를 pagecontroller(fragmentcontroll겸용)와 apicontroller로 구분하고 apicontroller는 JSON으로 회신하여 향후 프론트 변경 시 대응할 기반을 마련
  - 파일 업로드 기능을 표준화하여 재사용성 높이고 향후 타 파일 시스템과 연계를 염두에 두고 configuration설정을 도입 (NAS나 S3 변경 염두)
  - 사용자 편의성을 도모한다 (유사한 데이터를 복사하여 반복 입력 축소, 쉽게 검색 및 선택 가능한 picker, 마스터 데이터 생성 시 일괄 생성, Detail에서 프린터 기능을 도입하여 별도 문서작업 감소 등 )
  - 보안성을 확보한다

2. 계획
  각 단계는 사용자 확인 후 다음 단계를 진행한다.
  - HTML프론트 목업 수정 : 버튼,필드 배치나 기능성 검토
  - Entity 수정 : [memo,inspection,workorder,workpermit]-->ref_entity, ref_id, ref_stage, approvalId 추가하여 연관 데이터 추적성 강화 
  - 각 레이어별 표준화 
    1) Reposotiry 표준화: Page findbyCompanyId, page findbyCompanyIdAndPlantId(memo,inspection,workorder,workpermit), optional findByCompanyIdAnd[Module]ID, page findbyFilters
    2) Service 표준화: list, get, create, update, delete, getExisting, applyRequest(회신용), getItems, syncronizedItems, toItemEntity, resolveId, currentMemberId, submitPlanApproval(DRAFT), submitActualApproval(APPRV), onPlanApprovalApprove(APPRV), onPlanApprovalReject(REJCT), onPlanApprovalDelete, onActualApprovalApprove, onActualApprovalReject, onActualApprovalDelete, onPlanApprovalComplete(CMPLT:approval 모듈결재없이 자체 완료처리인 경우), onActualApprovalComplete, buildPlanApprovalContent, buildActualApprovalContent, prepareActualStage(PLN+APPRV → ACT+DRAFT)
    3) Controller 표준화: 기존 --> web.page/[모듈명]WebController.java, web.api/[모듈명]ApiController.java  [사용자 확인 후 전환된 구 Controller 삭제]
      * 조회는 thymeleaf, C/U/D는 api활용 예정이나, api에 조회 기능도 포함 (확장성 고려)
  - 코드별 중복되거나 오류가 있는 부분 수정 
    1) Javascript 검토 : 중복 초기화, initialize tag의 전역 변수 처리로 재 진입시 skip, 파라미터 개수나 type이나 null처리, 상호 의존성 등
    2) data 속성 검토 : data-* 중 안 쓰거나 명칭이 javscript와 html간 차이점 검토 

3. Task 및 Checklist

## 🎯 현재 상태 진단 (Current Status Diagnosis)

### ✅ 완료된 사항
- **Entity 구조**: 워크플로우 모듈(Inspection, WorkOrder, WorkPermit, Memo)에 `stage`, `status`, `ref_entity`, `ref_id`, `ref_stage`, `approvalId` 필드 추가 완료
  - ⚠️ InventoryTx는 거래 모듈로 stage/status 필드 없음 (Entity 변경 없음)
- **Repository 표준화**: 모든 업무 모듈 Repository에 `stage`, `status` 필터 추가, 명명 규칙 통일
- **Service Layer 표준화**: 계획/실적 워크플로우 메서드 완전 구현 (submitPlanApproval, onPlanApprovalApprove 등)
- **Controller 분리**: 업무 모듈 6개 (Inspection, WorkOrder, WorkPermit, Memo, Approval, InventoryTx) PageController + ApiController 분리 완료
  - 📁 web/page/ - HTML 반환 (Model + Thymeleaf)
  - 📁 web/api/ - JSON 반환 (ResponseEntity)
- **Approval 콜백**: ApprovalService에서 원본 모듈 콜백 메커니즘 완성 (PLN/ACT 분기 포함)
- **JavaScript 구조**: core/, ui/, pages/, api/ 폴더 구분 완료
- **HTML Fragment**: `th:fragment="content"` 구조 적용
- **파일 업로드**: file-upload, file-list 위젯 구현

## ✅ 마이그레이션 완료!

**완료 일시**: 2025-10-13
**총 작업 파일**: 40개 이상
**빌드 상태**: ✅ BUILD SUCCESSFUL

### 🎯 다음 단계
- [ ] **통합 테스트**: 전체 워크플로우 시나리오 테스트
- [ ] **크로스 브라우저 테스트**: Chrome, Edge, Firefox 테스트
- [ ] **사용자 인수 테스트**: 실제 운영 환경 시뮬레이션

---

## 📋 단계별 Task 및 Checklist

### **PHASE 1: Entity 보완 및 검증** ✅ 완료
> 목표: Entity 구조를 마이그레이션 계획에 맞게 완성하고 데이터 초기화 검증

#### Task 1.1: Memo Entity 보완 ✅
- [x] Memo.java에 `stage`, `status` 필드 추가
- [x] MemoId 복합키 확인

#### Task 1.2: Entity 검증 ✅
- [x] 워크플로우 모듈(Inspection, WorkOrder, WorkPermit, Memo) Entity 필드 검증
  - [x] stage: String(10) - PLN|ACT
  - [x] status: String(10) - DRAFT|SUBMT|PROC|APPRV|REJCT|CMPLT
  - [x] refEntity: String(10) - MEMO|INSP|WORK|WPER
  - [x] refId: String(10)
  - [x] refStage: String(10) - PLN|ACT
  - [x] approvalId: String(10)
- [x] ⚠️ InventoryTx는 Entity 변경 없음 (거래 모듈로 stage/status 불필요)

#### Task 1.3: DataInitializer 검증 ✅
- [x] DataInitializer.java의 초기 데이터가 stage/status 분리 아키텍처 반영 확인
- [x] 샘플 데이터 생성 로직 검증 (PLN+DRAFT, PLN+APPRV, ACT+DRAFT 등)
- [x] APPRV 코드, MODUL 코드, JOBTP 코드 정의 확인

**✅ Checkpoint 1**: Entity 구조 완성 및 데이터 초기화 검증 완료

---

### **PHASE 2: Repository Layer 표준화** ✅ 완료
> 목표: Repository 메서드 명명 규칙 통일 및 필수 메서드 구현

#### Task 2.1: InspectionRepository 표준화 ✅
- [x] `findByIdCompanyId` - 전체 조회
- [x] `findByIdCompanyIdAndPlantId` - plantId 필터
- [x] `findByIdCompanyIdAndIdInspectionId` - 단건 조회
- [x] `findByFilters` - stage, status 파라미터 포함
- [x] `search` - 키워드 검색

#### Task 2.2: WorkOrderRepository 표준화 ✅
- [x] 동일한 명명 규칙 적용
- [x] `findByFilters`에 **stage 파라미터 추가**

#### Task 2.3: WorkPermitRepository 표준화 ✅
- [x] 동일한 명명 규칙 적용
- [x] `findByFilters`에 **stage 파라미터 추가**

#### Task 2.4: MemoRepository 표준화 ✅
- [x] `findByIdCompanyIdAndPlantId` 메서드 추가
- [x] `findByFilters`에 **stage, status 파라미터 추가**

**✅ Checkpoint 2**: Repository 표준화 완료

---

### **PHASE 3: Service Layer 표준화 및 Approval 연계** ✅ 완료
> 목표: Service Layer 메서드 통일 및 워크플로우 구현

#### Task 3.1: InspectionService 표준화 ✅
- [x] 기본 CRUD 메서드
  - [ ] `list(companyId, filters, pageable)` - @Transactional(readOnly=true)
  - [ ] `get(companyId, inspectionId)` - @Transactional(readOnly=true)
  - [ ] `create(companyId, request)` - stage, status 설정
  - [ ] `update(companyId, inspectionId, request)`
  - [ ] `delete(companyId, inspectionId)`
  - [ ] `getExisting(companyId, inspectionId)` - 조회 후 없으면 예외
- [ ] 계획 워크플로우
  - [ ] `submitPlanApproval(companyId, inspectionId)` - status: DRAFT → SUBMT, Approval 생성
  - [ ] `onPlanApprovalApprove(companyId, inspectionId)` - status: SUBMT/PROC → APPRV (결재 승인 콜백)
  - [ ] `onPlanApprovalReject(companyId, inspectionId)` - status → REJCT (결재 반려 콜백)
  - [ ] `onPlanApprovalDelete(companyId, inspectionId)` - 결재 삭제 시 콜백
  - [ ] `onPlanApprovalComplete(companyId, inspectionId)` - status: DRAFT → CMPLT (결재 없이 자체 완료)
  - [ ] `buildPlanApprovalContent(inspection)` - 계획 결재 문서 내용 생성
  - [ ] `prepareActualStage(companyId, planId)` - 계획 복사 → 실적 생성 (stage:ACT, status:DRAFT)
- [ ] 실적 워크플로우
  - [ ] `submitActualApproval(companyId, inspectionId)` - status: DRAFT → SUBMT, Approval 생성
  - [ ] `onActualApprovalApprove(companyId, inspectionId)` - status: SUBMT/PROC → APPRV (결재 승인 콜백)
  - [ ] `onActualApprovalReject(companyId, inspectionId)` - status → REJCT (결재 반려 콜백)
  - [ ] `onActualApprovalDelete(companyId, inspectionId)` - 결재 삭제 시 콜백
  - [ ] `onActualApprovalComplete(companyId, inspectionId)` - status: DRAFT → CMPLT (결재 없이 자체 완료)
  - [ ] `buildActualApprovalContent(inspection)` - 실적 결재 문서 내용 생성
- [ ] Items 처리 메서드 (점검 항목)
  - [ ] `getItems(companyId, inspectionId)` - 점검 항목 조회
  - [ ] `syncronizedItems(companyId, inspectionId, itemRequests)` - 항목 일괄 동기화
  - [ ] `toItemEntity(itemRequest)` - Request → Entity 변환
- [ ] 유틸리티 메서드
  - [ ] `applyRequest(entity, request)` - Request 데이터를 Entity에 적용
  - [ ] `resolveId(companyId, moduleCode)` - 새로운 ID 생성
  - [ ] `currentMemberId()` - 현재 로그인 사용자 ID 조회

#### Task 3.2: WorkOrderService 표준화 ✅
- [x] 기본 CRUD 메서드 (list, get, create, update, delete, getExisting)
- [ ] 계획 워크플로우
  - [ ] submitPlanApproval, onPlanApprovalApprove, onPlanApprovalReject
  - [ ] onPlanApprovalDelete, onPlanApprovalComplete, buildPlanApprovalContent
  - [ ] prepareActualStage (계획 복사 → 실적)
- [ ] 실적 워크플로우
  - [ ] submitActualApproval, onActualApprovalApprove, onActualApprovalReject
  - [ ] onActualApprovalDelete, onActualApprovalComplete, buildActualApprovalContent
- [ ] Items 처리 (getItems, syncronizedItems, toItemEntity)
- [ ] 유틸리티 (applyRequest, resolveId, currentMemberId)

#### Task 3.3: WorkPermitService 표준화 ✅
- [x] 기본 CRUD 메서드 (list, get, create, update, delete, getExisting)
- [ ] 계획 워크플로우 (계획만 존재, 실적 없음)
  - [ ] submitPlanApproval, onPlanApprovalApprove, onPlanApprovalReject
  - [ ] onPlanApprovalDelete, onPlanApprovalComplete, buildPlanApprovalContent
- [ ] Checklist 처리 (checksheetJson 필드 관리)
- [ ] 유틸리티 (applyRequest, resolveId, currentMemberId)

#### Task 3.4: MemoService 표준화 ✅
- [x] 기본 CRUD 메서드
- [x] list 메서드에 stage/status 파라미터 추가

#### Task 3.5: ApprovalService 콜백 구현 ✅
- [x] `approve(approvalId, comment)` - 결재 승인 시 원본 모듈 콜백
- [x] `reject(approvalId, comment)` - 결재 반려 시 원본 모듈 콜백
- [x] `delete(approvalId)` - 결재 삭제 시 원본 모듈 콜백
- [x] notifyRefModule 메서드에서 INSP, WORK, WPER stage 분기 처리

**✅ Checkpoint 3**: Service Layer 표준화 및 워크플로우 구현 완료

---

### **PHASE 4: Controller 분리 (Page/API)** ✅ 완료
> 목표: 업무 모듈 Controller를 PageController(HTML+데이터)와 ApiController(JSON)로 분리

**⚠️ 분리 대상**: 업무 모듈 6개만 (Inspection, WorkOrder, WorkPermit, Memo, Approval, InventoryTx)

**✅ 유지 대상 (API 분리 안 함)**:
- 도메인 모듈: Company, Dept, Func, Member, Role, Site, Storage
- 코드 모듈: Code (CodeType, CodeItem)
- 마스터 데이터: Plant, Inventory
- 시스템/공통: Auth, Csrf, Health, Layout

#### Task 4.1: 폴더 구조 생성 ✅
- [x] `src/main/java/com/cmms11/web/page/` 폴더 생성
- [x] `src/main/java/com/cmms11/web/api/` 폴더 생성

#### Task 4.2: InspectionController 분리 ✅
- [x] `InspectionPageController.java` 생성 (패키지: com.cmms11.web.page)
  - [ ] `GET /inspection/list` - 목록 조회, Model에 데이터 담아 HTML 반환
  - [ ] `GET /inspection/detail/{id}` - 상세 조회, Model에 데이터 담아 HTML 반환
  - [ ] `GET /inspection/form` - 등록/수정 폼, Model에 selectbox 데이터 담아 HTML 반환
    - [ ] `?stage=PLN` - 계획 입력
    - [ ] `?stage=ACT&ref_entity=INSP&ref_id=xxx&ref_stage=PLN` - 실적 입력 (계획 복사)
    - [ ] `?id=xxx` - 수정 모드
  - [ ] `GET /inspection/plan` - 계획 목록 HTML 반환
  - [ ] Fragment 지원: `_fragment=true` 파라미터 처리
- [ ] `InspectionApiController.java` 생성 (패키지: com.cmms11.web.api)
  - [ ] `POST /api/inspections` - 생성 (stage, status 설정)
  - [ ] `PUT /api/inspections/{id}` - 수정
  - [ ] `DELETE /api/inspections/{id}` - 삭제
  - [ ] `POST /api/inspections/{id}/submit-plan-approval` - 계획 결재 상신
  - [ ] `POST /api/inspections/{id}/confirm-plan` - 계획 자체 확정 (결재 없이)
  - [ ] `POST /api/inspections/{id}/prepare-actual` - 실적 입력 준비 (계획 복사)
  - [ ] `POST /api/inspections/{id}/submit-actual-approval` - 실적 결재 상신
  - [ ] `GET /api/inspections/{id}/items` - 점검 항목 조회
  - [ ] ResponseEntity<T> JSON 반환
- [ ] 기존 `InspectionController.java` 삭제 (사용자 확인 후)

#### Task 4.3: WorkOrderController 분리 ✅
- [x] `WorkOrderPageController.java` 생성
  - [ ] `GET /workorder/list`, `GET /workorder/detail/{id}`, `GET /workorder/form`
  - [ ] Fragment 지원
- [ ] `WorkOrderApiController.java` 생성
  - [ ] CRUD: `POST /api/workorders`, `PUT /api/workorders/{id}`, `DELETE /api/workorders/{id}`
  - [ ] 계획: `POST /api/workorders/{id}/submit-plan-approval`, `POST /api/workorders/{id}/confirm-plan`
  - [ ] 실적: `POST /api/workorders/{id}/prepare-actual`, `POST /api/workorders/{id}/submit-actual-approval`
  - [ ] Items: `GET /api/workorders/{id}/items`
- [ ] 기존 `WorkOrderController.java` 삭제 (사용자 확인 후)

#### Task 4.4: WorkPermitController 분리 ✅
- [x] `WorkPermitPageController.java` 생성
  - [ ] `GET /workpermit/list`, `GET /workpermit/detail/{id}`, `GET /workpermit/form`
  - [ ] Fragment 지원
- [ ] `WorkPermitApiController.java` 생성
  - [ ] CRUD: `POST /api/workpermits`, `PUT /api/workpermits/{id}`, `DELETE /api/workpermits/{id}`
  - [ ] 계획만: `POST /api/workpermits/{id}/submit-plan-approval`, `POST /api/workpermits/{id}/confirm-plan`
  - [ ] ⚠️ 실적 입력 API 없음 (workpermit은 실적 없음)
- [ ] 기존 `WorkPermitController.java` 삭제 (사용자 확인 후)

#### Task 4.5: MemoController 분리 ✅
- [x] `MemoPageController.java` 생성
  - [ ] `GET /memo/list`, `GET /memo/detail/{id}`, `GET /memo/form`
  - [ ] Fragment 지원
- [ ] `MemoApiController.java` 생성
  - [ ] CRUD: `POST /api/memos`, `PUT /api/memos/{id}`, `DELETE /api/memos/{id}`
  - [ ] ⚠️ Memo는 stage/status 없음 (단순 CRUD만)
- [ ] 기존 `MemoController.java` 삭제 (사용자 확인 후)

#### Task 4.6: ApprovalController 분리 ✅
- [x] `ApprovalPageController.java` 생성
  - [ ] `GET /approval/list`, `GET /approval/detail/{id}`, `GET /approval/form`
  - [ ] Fragment 지원
- [x] `ApprovalApiController.java` 생성
  - [x] `POST /api/approvals/{id}/approve` - 결재 승인 (원본 모듈 콜백 호출)
  - [x] `POST /api/approvals/{id}/reject` - 결재 반려 (원본 모듈 콜백 호출)
  - [x] `DELETE /api/approvals/{id}` - 결재 삭제 (원본 모듈 콜백 호출)
- [ ] 기존 `ApprovalController.java` 삭제 (사용자 확인 후)

#### Task 4.7: InventoryTxController 분리 ✅ (추가)
- [x] `InventoryTxPageController.java` 생성
  - [x] `GET /inventoryTx/transaction`, `GET /inventoryTx/closing`, `GET /inventoryTx/ledger`
  - [x] Fragment 지원
- [x] `InventoryTxApiController.java` 생성
  - [x] `POST /api/inventoryTx/transaction` - 거래 등록
  - [x] `POST /api/inventoryTx/closing` - 월별 마감
  - [x] `GET /api/inventoryTx/ledger` - 원장 조회
  - [x] `GET /api/inventoryTx/stock/*` - 재고 현황 조회
  - [x] ⚠️ InventoryTx는 Entity 변경 없음 (stage/status 불필요)
- [ ] 기존 `InventoryTxController.java` 삭제 (사용자 확인 후)

**⚠️ 기존 Controller 삭제 대기 (6개)**:
- [ ] InspectionController.java
- [ ] WorkOrderController.java
- [ ] WorkPermitController.java
- [ ] MemoController.java
- [ ] ApprovalController.java
- [ ] InventoryTxController.java

**✅ Checkpoint 4**: Controller 분리 완료 (기존 Controller 삭제 대기)

---

### **PHASE 5: HTML 템플릿 표준화** ✅ 완료
> 목표: HTML 템플릿에 stage/status 분리 아키텍처 반영 및 data-form-manager 적용

**⚠️ 중요 구분**:
- **업무 모듈 (6개)**: `data-form-manager` 사용, `th:action`/`method` 제거 → API 호출
- **도메인/코드/마스터**: `th:action`/`method` 유지, **모든 data-* 속성 제거** → 서버 직접 처리

#### Task 5.0: 도메인/코드/마스터 템플릿 정리 ✅
- [x] 모든 비업무 모듈 템플릿에서 **data-* 속성 완전 제거**
  - [ ] **도메인 모듈**: `domain/company/`, `domain/dept/`, `domain/func/`, `domain/member/`, `domain/role/`, `domain/site/`, `domain/storage/`
  - [ ] **코드 모듈**: `code/form.html`, `code/list.html`
  - [ ] **마스터 데이터**: `plant/`, `inventory/`
  - [ ] form에서 `data-validate`, `data-form-manager` 등 **모든 data-* 속성 제거**
  - [ ] 순수 HTML form으로: `<form method="post" th:action="@{/path}">`
  - [ ] HTML5 validation만 사용 (required, maxlength 유지)

#### Task 5.1: inspection/form.html 개선 ✅
- [x] `data-form-manager` 속성 추가
- [x] `data-validate` 삭제
- [x] `data-action`, `data-method`, `data-redirect` 설정
- [x] stage/status hidden 필드 추가
- [x] refEntity, refId, refStage 필드 추가 (수정 가능)
- [x] status 기반 버튼 활성화 조건 (th:if → th:classappend로 색상만 변경)
- [x] 기존 th:action, method="post" 제거

#### Task 5.2: inspection/detail.html 개선 ✅
- [x] stage/status 표시 로직 개선 (PLN/APPRV로 분리 표시)
- [x] 버튼 구성 (버튼 삭제하지 말고 색상만 변경 gray/blue):
  - [x] "목록" - 항상 활성화
  - [x] "인쇄" - 항상 활성화
  - [x] "수정" - status=DRAFT일 때만 활성화
  - [x] "결재 상신" - status=DRAFT일 때만 활성화 (PLN/ACT 무관)
  - [x] "실적 입력" - stage=PLN AND status=APPRV일 때만 활성화 (→ form?stage=ACT&ref_entity=INSP&ref_id=xxx&ref_stage=PLN)

#### Task 5.3~5.8: 나머지 모듈 HTML 템플릿 개선 ✅
- [x] **inspection/list.html**: stage/status 필터 및 분리 표시
- [x] **workorder/**: form.html, detail.html, list.html 표준화
- [x] **workpermit/**: form.html, detail.html, list.html 표준화 (실적 입력 버튼 없음)
- [x] **memo/**: form.html, detail.html, list.html 표준화 (작업지시 연결 버튼 추가)
- [x] **approval/**: form.html, detail.html 표준화
- [x] **inventoryTx/**: transaction.html, closing.html, ledger.html 표준화

**✅ Checkpoint 5**: HTML 템플릿 표준화 완료

---

### **PHASE 6: JavaScript 개선 및 form-manager 통합** ✅ 완료
> 목표: navigation.js의 handleSPAForms() 구현 및 모듈별 JS 정리

#### Task 6.1: navigation.js 구현 ✅
- [x] `handleSPAForms()` 메서드 구현 (신규 추가)
- [x] `data-form-manager` 폼 자동 바인딩 로직 구현
- [x] FormData → JSON 변환 로직
- [x] items 배열 처리 로직
- [x] `{id}` 치환 로직 구현
- [x] 파일 업로드 통합
- [x] data-validate 레거시 로직 제거

#### Task 6.2: pages/inspection.js 개선 ✅
- [x] submitApproval 전역 함수 추가 (모든 모듈에서 공통 사용)
- [x] initApprovalButtons() 메서드 추가
- [x] 중복 초기화 방지 유지 (재진입 가드)
- [x] Thymeleaf에서 POST/PUT 자동 전환 (data-method 동적 변경 불필요)

#### Task 6.3: pages/workorder.js 개선 ✅
- [x] initApprovalButtons() 메서드 추가
- [x] 공통 submitApproval 함수 사용

#### Task 6.4: pages/workpermit.js 개선 ✅
- [x] initApprovalButtons() 메서드 추가
- [x] 공통 submitApproval 함수 사용

#### Task 6.5: pages/memo.js, inventoryTx.js ✅
- [x] 특별한 처리 없음 (data-form-manager가 자동 처리)

**✅ Checkpoint 6**: JavaScript 개선 완료

---

### **PHASE 7: 통합 테스트 및 검증**
> 목표: 전체 워크플로우 시나리오 테스트

#### Task 7.1: Inspection 워크플로우 테스트
- [ ] 계획 입력 → 저장 (PLN, DRAFT)
- [ ] 계획 확정 (PLN, APPRV)
- [ ] 실적 입력 (ACT, DRAFT) - 계획 복사
- [ ] 실적 결재 상신 (ACT, SUBMT)
- [ ] 결재 승인 (ACT, APPRV)
- [ ] 결재 반려 (ACT, REJCT)

#### Task 7.2: WorkOrder 워크플로우 테스트
- [ ] Memo에서 작업지시 생성 (refEntity=MEMO)
- [ ] 계획 입력/확정
- [ ] 실적 입력/승인

#### Task 7.3: WorkPermit 워크플로우 테스트
- [ ] 계획 입력/확정
- [ ] 결재 상신/승인

#### Task 7.4: 크로스 브라우저 테스트
- [ ] Chrome
- [ ] Edge
- [ ] Firefox

#### Task 7.5: 린터 오류 확인 및 수정
- [ ] Java 코드 린터 확인
- [ ] HTML 검증
- [ ] JavaScript 검증

**✋ Checkpoint 7**: 통합 테스트 완료 → 최종 사용자 승인

---

4. 기타 참고 사항 

## 개요 및 핵심 변경사항

### 🎯 주요 변경사항

#### 1. **로그인 및 멀티 컴퍼니**
- 로그인 화면: 정적 HTML + JavaScript (회사 선택 드롭다운)
- 인증: DB 연계 (`companyId:username` 형식)
- 회사 목록: CHROK, HPS, KEPS, OES (정적 관리)

#### 2. **Stage/Status 분리 아키텍처** ⭐ 핵심
```
대상은 업무모듈임(inspection, workorder, workpermit, memo, approval, inventoryTx), 마스터데이터나 도메인 코드는 대상 아님 
기존: status = "PLN_DRAFT", "PLN_APPRV", "ACT_DRAFT"
변경: stage = "PLN" | "ACT"
      status = "DRAFT" | "SUBMT" | "PROC" | "APPRV" | "REJCT" | "CMPLT"
```

#### 3. **모듈 간 참조 구조**
```
inspection, workorder, workpermit, memo, approval에 다음 필드 추가

stage: 원본의 단계 {PLN|ACT}
ref_entity: 참조 모듈 (INSP, WORK, MEMO 등 - 5자 코드)
ref_id: 참조 ID (원본 데이터의 ID)
ref_stage: 참조 단계 {PLN|ACT}
```

#### 4. **Controller 분리** (최소 변경)
```
기존: web/InspectionController.java
변경: 
  - web/page/InspectionPageController.java  (HTML + 데이터)
  - web/api/InspectionApiController.java    (JSON만)
```

#### 5. **데이터 처리 방식** 
```
PageController: 
  - 데이터 조회 (Service 호출)
  - Model에 담아 전달
  - 서버 렌더링 (th:value, th:each)
  
ApiController:
  - CRUD만 처리 (JSON)
  - 저장/수정/삭제
  
JavaScript:
  - form-manager만 사용
  - 데이터 로딩 코드 없음 (서버가 채움)
```

---

## 로그인 및 멀티 컴퍼니

### 로그인 흐름

```
정적 HTML 로그인 페이지
  ↓ 회사 선택: CHROK, HPS, KEPS, OES
  ↓ POST /api/auth/login
  ↓ username = "companyId:memberId"
  ↓ DB 인증
  ↓ 세션 생성
  ↓ /layout/defaultLayout.html?content=/memo/list
```

### 정적 로그인 HTML

```html
<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8" />
  <title>CMMS 로그인</title>
  <link rel="stylesheet" href="/assets/css/base.css" />
</head>
<body class="login-page">
  <div class="login-container">
    <form id="loginForm">
      <div class="form-row">
        <label>회사</label>
        <select name="companyId" required>
          <option value="">선택하세요</option>
          <option value="CHROK">초록에너지 (CHROK)</option>
          <option value="HPS">한국플랜트서비스 (HPS)</option>
          <option value="KEPS">한국발전기술 (KEPS)</option>
          <option value="OES">옵티멀에너지서비스 (OES)</option>
        </select>
      </div>
      
      <div class="form-row">
        <label>사용자명</label>
        <input name="username" required />
      </div>
      
      <div class="form-row">
        <label>비밀번호</label>
        <input name="password" type="password" required />
      </div>
      
      <button type="submit">로그인</button>
    </form>
  </div>
  
  <script>
    document.getElementById('loginForm').addEventListener('submit', async (e) => {
      e.preventDefault();
      const formData = new FormData(e.target);
      
      try {
        const response = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            companyId: formData.get('companyId'),
            username: formData.get('username'),
            password: formData.get('password')
          })
        });
        
        if (response.ok) {
          window.location.href = '/layout/defaultLayout.html?content=/memo/list';
        }
      } catch (error) {
        alert('로그인 실패');
      }
    });
  </script>
</body>
</html>
```

---

## Stage/Status 분리 아키텍처

### 핵심 개념

```
기존: status = "PLN_DRAFT", "PLN_APPRV"
변경: stage = "PLN", status = "DRAFT"
```

### URL 패턴 및 데이터 설정

#### URL 패턴
```
계획 입력:
  /inspection/form?stage=PLN

실적 입력:
  /inspection/form?stage=ACT

계획 복사 실적:
  /inspection/form?stage=ACT&ref_entity=[모듈명]&ref_id=INSP001&ref_stage={APPRV|CMPLT}

계획 수정:
  /inspection/form?stage=PLN&id=INSP001

실적 수정:
  /inspection/form?stage=ACT&id=INSP002
```


### 데이터설정 : Datainitialier.java 참고 

### 워크플로우

```
[계획 단계]
  stage:PLN, status:DRAFT
    ↓ 저장
  stage:PLN, status:DRAFT
    ↓ 확정 (결재 없음)
  stage:PLN, status:APPRV
    ↓ "실적입력" 버튼
    ↓ /inspection/form?stage=ACT&ref_id=INSP001
  
[실적 단계]
  stage:ACT, status:DRAFT (계획 복사)
    ↓ 저장
  stage:ACT, status:DRAFT
    ↓ 상신 (결재 시작)
  stage:ACT, status:SUBMT
    ↓ 결재 중
  stage:ACT, status:PROC
    ↓ 결재 승인
  stage:ACT, status:APPRV (완료)
```

## Controller 분리 전략

### 핵심 원칙 

```
PageController:
  ✅ 데이터 조회 (Service 호출-->form신규 시 select box 채움, form수정 모드, detail채움 등 )
  ✅ Model에 담아 전달
  ✅ 서버 렌더링 (th:value, th:each)
  ✅ Fragment 지원
  ❌ 저장/수정/삭제 없음

ApiController:
  ✅ CRUD 처리 (JSON)
  ✅ 워크플로우 API
  ✅ PageController와 동일한 Service Layer 활용 
```

## HTML 템플릿 표준 구조

### 기본 구조 (완전한 HTML)

```html
<!doctype html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="utf-8" />
  <title th:text="#{page.inspection.form}">점검 등록</title>
  <link rel="stylesheet" href="/assets/css/base.css" />
</head>
<body>
  <!-- ⭐ Fragment 정의 -->
  <section th:fragment="content" 
           data-slot-root 
           data-page="inspection-form" 
           data-module="inspection">
    <main>
      <div class="container">
        <section class="card">
          <div class="card-header">
            <h1 class="card-title" th:text="#{page.inspection.form}">점검 등록</h1>
          </div>
          
          <div class="card-body">
            <!-- ⭐ form-manager가 자동 바인딩 -->
            <form data-form-manager 
                  data-action="/api/inspections"
                  data-method="POST"
                  data-redirect="/inspection/detail/{id}"
                  th:data-is-new="${isNew}"
                  th:data-inspection-id="${inspection?.inspectionId}">
              
              <!-- Hidden fields -->
              <input type="hidden" name="inspectionId" th:value="${inspection?.inspectionId}" />
              <input type="hidden" name="stage" th:value="${stage}" />
              <input type="hidden" name="status" value="DRAFT" />
              
              <!-- ⭐ 참조 정보 (수정 가능) -->
              <div class="form-row">
                <label>참조 엔티티</label>
                <select name="refEntity">
                  <option value="">없음</option>
                  <option value="MEMO" th:selected="${inspection?.refEntity == 'MEMO'}">메모</option>
                  <option value="INSP" th:selected="${inspection?.refEntity == 'INSP'}">점검</option>
                  <option value="WORK" th:selected="${inspection?.refEntity == 'WORK'}">작업지시</option>
                  <option value="WPER" th:selected="${inspection?.refEntity == 'WPER'}">작업허가</option>
                </select>
              </div>
<!-- 중간 생략 -->

<!--
    ⭐ Thymeleaf 처리:
    - th:fragment="content": Fragment 추출 지점
    - th:value: 서버가 값 채움
    - th:each: 서버가 옵션 생성
    - th:selected: 서버가 선택 설정
    
    ⭐ PageController return:
    - _fragment=true → "inspection/form :: content" (Fragment만)
    - _fragment=false → "inspection/form" (전체 페이지)
    
    ⭐ form-manager 처리(navigation.js 내 위치):
    - data-redirect="/inspection/detail/{id}" - 저장 후 상세로 이동
    - {id}는 응답의 inspectionId로 자동 치환
    - PageController는 리다이렉트 안 함
    
    ⭐ 전용 JS 처리:
    - form의 data-is-new, data-inspection-id를 읽어서 
    - 수정 모드일 경우 data-method와 data-action을 동적 변경
    - inspection.js 참조
-->
  
  <script src="/assets/js/inspection.js"></script>
</body>
</html>
```

### 화면 구성

버튼 활성화 여부는 th:if로 판단. 버튼 없애지 말고 색 변경 (blue-->gray). api에 체크로직 추가 
#### memo
detail에서 작업지시 버튼 (목록, 인쇄, 수정 외) --> workorder/form에 ref_entity:memo, ref_id:memoId, ref_stage:CMPLT

### inspection
plan에서 저장과 확정 버튼 : stage=PLN, status={저장:DRAFT|확정:APPRV}
form은 DRAFT일때만 수정(목록,수정 버튼 외). ref_entity, ref_id, ref_stage 필드 추가 
detail은 결재상신 (DRAFT일때, PLN/ACT 무관). 목록, 수정, 인쇄 버튼 외. 실적 입력(PLN, APPRV일때만) 

### workorder 
detail에서 결재 상신(DRAFT일때, PLN/ACT 무관), 실적 입력(PLN, APPRV일때만). 목록, 수정, 인쇄 버튼 외 

### workpermit 
detail에서 결재 상신(DRAFT일때, PLN/ACT 무관). 목록, 수정, 인쇄 버튼 외. ⚠️ 실적 입력 버튼 없음 

**워크플로우 예시**:
```
[계획 확정 상태]
PLN, APPROV
  ↓ "실적입력" 버튼 클릭
  ↓ /inspection/form?stage=ACT&ref_id=INSP001&ref_entity=INSP&ref_stage=PLN
  
[폼 페이지]
  ↓ 서버가 planData.withNullId() 처리
  ↓ 계획 데이터로 폼 채워짐 (ID는 null)
  ↓ 사용자 수정 후 저장
  
[실적 생성]
  ↓ 새로운 inspectionId 발급
  ↓ stage=ACT, status=DRAFT, refId=계획ID 저장
  ↓ detail로 redirect
  
[결재 상신]
  ↓ Approval 모듈에서 처리 (별도 화면) / 결재 완료 시 원 모듈(ref) APPRV. 반려시 DRAFT 변경 
  ↓ 상신 완료 후 list로 redirect

```
## 기타 

### JAVASCRIPT 참조 
**상세한 JavaScript 가이드는 [CMMS_JAVASCRIPT.md](./CMMS_JAVASCRIPT.md)를 참조하세요.**
**⚠️ 중요: HTML 속성은 동일**
```html
<!-- data-form-manager 속성은 그대로 사용! -->
<form data-form-manager
      data-action="/api/inspections" 
      data-method="POST" 
      data-redirect="/inspection/list">
  <button type="submit">저장</button>
</form>
```

**동작 흐름**:
```javascript
// core/navigation.js
loadContent(url) {
  // 1. HTML 로드 (fetch)
  // 2. 슬롯에 삽입 (slot.innerHTML = html)
  // 3. 페이지 모듈 로드 (loadModule)
  // 4. handleSPAForms() ← [data-form-manager] 폼 자동 바인딩 ⭐
  // 5. 위젯 초기화 (file-upload, file-list)
}

**⚠️ 중요: 자동 바인딩 메커니즘**

폼 제출 처리는 **자동으로 바인딩**됩니다:

```javascript
// ✅ 자동 실행: navigation.js가 콘텐츠 로드 시 자동 호출
loadContent(url) {
  fetch(url).then(html => {
    slot.innerHTML = html;
    handleSPAForms();  // ← 자동 호출! 수동 호출 불필요
  });
}
```

### API 권한 (주석 처리 - 추후 고도화)

```java
@RestController
public class InspectionApiController {
    
    // @PreAuthorize("hasAuthority('INSPECTION_R')")
    @GetMapping("/{id}")
    public ResponseEntity<InspectionResponse> get(...) { }
    
    // @PreAuthorize("hasAuthority('INSPECTION_C')")
    @PostMapping
    public ResponseEntity<InspectionResponse> create(...) { }
    
    // @PreAuthorize("hasAuthority('INSPECTION_U')")
    @PutMapping("/{id}")
    public ResponseEntity<InspectionResponse> update(...) { }
    
    // @PreAuthorize("hasAuthority('INSPECTION_D')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(...) { }
}
```

## 다국어(i18n) 처리

### 기본 원칙

```
서버 처리: 모든 레이블, 값 (th:text, th:value)
JavaScript: 동적 메시지만 (알림, 확인)
```

### messages.properties

```
properties
# messages_ko.properties
page.inspection.form=점검 등록
label.inspection.name=점검명
label.plant=설비
label.stage=단계
label.status=상태
placeholder.inspection.name=점검명을 입력하세요
button.save=저장
button.cancel=취소
```

### 서비스 Layer는 가능한 기존 유지하고 신규 기능만 업데이트 

### 결재 승인/반려 콜백

```java
// ApprovalService.java
public void approve(String approvalId, String comment) {
    String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
    Approval approval = findApproval(companyId, approvalId);
    approval.setStatus("APPRV");
    repository.save;
    
    // ⭐ 업무 모듈 콜백
    if ("INSP".equals(approval.getRefEntity())) {
        inspectionService.onApprovalApproved(companyId, approval.getRefId());
        // → status:APPRV
    } else if ("WORK".equals(approval.getRefEntity())) {
        workOrderService.onApprovalApproved(companyId, approval.getRefId());
    }
}

public void reject(String companyId, String approvalId, String comment) {
    Approval approval = findApproval(companyId, approvalId);
    approval.setStatus("REJCT");
    approvalRepository.save(approval);
    
    // ⭐ 업무 모듈 콜백
    if ("INSP".equals(approval.getRefEntity())) {
        inspectionService.onApprovalRejected(companyId, approval.getRefId());
        // → status:REJCT
    }
}
```

## 기타 참고 가이드

### HTML 템플릿 작성 포인트
- `section[data-slot-root]`와 `th:fragment` 조합으로 SPA 슬롯 및 전체 페이지 렌더링 모두 대응
- `form[data-form-manager]`에 `data-action`/`data-method`/`data-redirect`를 명시하고 숨김 필드로 stage/status/ref 값을 전달
- 버튼 활성화 조건은 `th:if` 혹은 `th:classappend`로 stage/status를 기준 제어하고 색상만 변경하여 레이아웃 유지

### 페이지/API 컨트롤러 분리 포인트
- PageController는 `Model` 또는 `ModelAndView`로 템플릿 이름과 필수 데이터를 반환하고 저장 로직은 포함하지 않음
- ApiController는 `ResponseEntity<T>`로 CRUD 및 워크플로우 응답(JSON)을 반환하며 PageController와 동일한 Service를 호출
- `_fragment=true` 요청 시 PageController는 `template :: fragment` 형식으로 조각만 반환하여 SPA 네비게이션과 호환

### Repository/Service 표준 포인트
- Repository는 `findByCompanyId`, `findByCompanyIdAndPlantId`, `findByCompanyIdAnd...Id` 등 prefix 규칙을 유지하고 Page 조회는 Pageable을 활용
- Service는 `list/get/create/update/delete` 기본 메서드 외에 `submitPlanApproval`, `submitActualApproval` 등 Approval 연계 메서드를 노출
- 읽기 전용 메서드는 `@Transactional(readOnly = true)`로, 상태 변경 메서드는 명시적 트랜잭션 경계를 두어 stage/status 업데이트를 보장

### JavaScript 초기화 포인트
- `navigation.js`가 콘텐츠 로딩 후 `handleSPAForms()`를 자동 호출하므로 개별 화면에서 중복 초기화 호출을 피함
- `data-page`나 `data-module` 기준으로 재진입 시 초기화를 건너뛰는 가드를 두고 전역 변수 사용을 최소화
- 폼 저장 후 응답의 `{id}` 치환, 파일 업로드 위젯 등 공통 유틸을 우선 활용하고 필요 시 전용 모듈에서 추가 초기화 수행

### Approval 연계 포인트
- `submitPlanApproval` → Approval 생성 → `onPlanApprovalApprove/onPlanApprovalReject` 콜백 순서를 문서화하여 stage/status 전이를 명확히 함
- 실적 단계는 `submitActualApproval`과 `onActualApproval*` 콜백으로 처리하고 반려 시 원본 status를 `REJCT` 또는 `DRAFT`로 롤백
- Approval 엔터티의 `refEntity/refId/refStage` 값을 이용해 업무 모듈 Service에서 대상 레코드를 식별하고 후속 처리를 수행

