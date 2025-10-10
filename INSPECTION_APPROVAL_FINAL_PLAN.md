# Inspection-Approval 통합 구현 최종 계획서

## 📋 개요

### **핵심 흐름**
```
inspection/form.html [결재 상신]
    ↓
Approval 생성 (DRAFT, 빈 결재선)
Inspection.status = SUBMIT
    ↓
사용자가 결재선 입력 후 [상신]
Approval.status = SUBMIT
    ↓
결재자가 승인/반려
    ↓
원본 모듈 상태 자동 변경
```

---

## 1️⃣ DB 마이그레이션

```sql
-- inspection 테이블에 approval_id 추가
ALTER TABLE inspection 
ADD COLUMN approval_id CHAR(10) AFTER status;

CREATE INDEX ix_inspection_approval ON inspection(company_id, approval_id);

-- 향후 확장을 위한 다른 모듈도 추가
ALTER TABLE work_order ADD COLUMN approval_id CHAR(10) AFTER status;
ALTER TABLE work_permit ADD COLUMN approval_id CHAR(10) AFTER status;
```

---

## 2️⃣ Backend 구현

### **A. Inspection.java**
```java
// src/main/java/com/cmms11/inspection/Inspection.java

@Column(name = "approval_id", length = 10)
private String approvalId;
```

### **B. InspectionRequest.java**
```java
// src/main/java/com/cmms11/inspection/InspectionRequest.java

public record InspectionRequest(
    @Size(max = 10) String inspectionId,
    @Size(max = 100) String name,
    @Size(max = 10) String plantId,
    @Size(max = 5) String jobId,
    @Size(max = 5) String siteId,
    @Size(max = 5) String deptId,
    @Size(max = 5) String memberId,
    LocalDate plannedDate,
    LocalDate actualDate,
    @Size(max = 10) String status,
    @Size(max = 10) String fileGroupId,
    @Size(max = 500) String note,
    @Size(max = 10) String approvalId,  // ⭐ 추가
    @Valid List<InspectionItemRequest> items
) {}
```

### **C. InspectionService.java**
```java
// src/main/java/com/cmms11/inspection/InspectionService.java

@Autowired
private ApprovalService approvalService;

/**
 * 결재 임시저장 (빈 결재선)
 */
public ApprovalResponse saveDraftApproval(String inspectionId) {
    Inspection inspection = getExisting(inspectionId);
    
    if (!"PROC".equals(inspection.getStatus())) {
        throw new IllegalStateException("진행 중인 점검만 결재 요청 가능합니다.");
    }
    
    // 결재 본문 자동 생성
    String content = buildApprovalContent(inspection);
    
    // 빈 결재선으로 Approval 생성
    ApprovalRequest request = new ApprovalRequest(
        null,
        "점검 결재: " + inspection.getName(),
        "DRAFT",
        "INSP",
        inspectionId,
        content,
        inspection.getFileGroupId(),
        new ArrayList<>()  // 빈 결재선
    );
    
    ApprovalResponse approval = approvalService.create(request);
    
    // Inspection 업데이트
    inspection.setApprovalId(approval.approvalId());
    inspection.setStatus("SUBMIT");
    inspection.setUpdatedAt(LocalDateTime.now());
    inspection.setUpdatedBy(currentMemberId());
    repository.save(inspection);
    
    return approval;
}

/**
 * 결재 완료 콜백
 */
public void onApprovalComplete(String inspectionId) {
    Inspection inspection = getExisting(inspectionId);
    inspection.setStatus("COMPLETE");
    inspection.setUpdatedAt(LocalDateTime.now());
    inspection.setUpdatedBy(currentMemberId());
    repository.save(inspection);
}

/**
 * 결재 반려 콜백
 */
public void onApprovalReject(String inspectionId) {
    Inspection inspection = getExisting(inspectionId);
    inspection.setStatus("REJECT");
    inspection.setUpdatedAt(LocalDateTime.now());
    inspection.setUpdatedBy(currentMemberId());
    repository.save(inspection);
}

/**
 * 결재 본문 생성
 */
private String buildApprovalContent(Inspection inspection) {
    StringBuilder sb = new StringBuilder();
    sb.append("<h3>점검 결재 요청</h3>");
    sb.append("<table style='border-collapse:collapse; width:100%;'>");
    sb.append("<tr><th style='border:1px solid #ddd; padding:8px; background:#f5f5f5;'>항목</th>");
    sb.append("<th style='border:1px solid #ddd; padding:8px; background:#f5f5f5;'>내용</th></tr>");
    
    sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>점검 ID</td>");
    sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getId().getInspectionId()).append("</td></tr>");
    
    sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>점검명</td>");
    sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getName()).append("</td></tr>");
    
    sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>설비</td>");
    sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getPlantId() != null ? inspection.getPlantId() : "-").append("</td></tr>");
    
    sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>담당자</td>");
    sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getMemberId() != null ? inspection.getMemberId() : "-").append("</td></tr>");
    
    sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>계획일</td>");
    sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getPlannedDate() != null ? inspection.getPlannedDate() : "-").append("</td></tr>");
    
    sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>실적일</td>");
    sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getActualDate() != null ? inspection.getActualDate() : "-").append("</td></tr>");
    
    sb.append("</table>");
    
    if (inspection.getNote() != null && !inspection.getNote().isEmpty()) {
        sb.append("<p style='margin-top:15px;'><strong>비고:</strong></p>");
        sb.append("<p>").append(inspection.getNote()).append("</p>");
    }
    
    return sb.toString();
}

private void applyRequest(Inspection entity, InspectionRequest request) {
    entity.setName(request.name());
    entity.setPlantId(request.plantId());
    entity.setJobId(request.jobId());
    entity.setSiteId(request.siteId());
    entity.setDeptId(request.deptId());
    entity.setMemberId(request.memberId());
    entity.setPlannedDate(request.plannedDate());
    entity.setActualDate(request.actualDate());
    entity.setStatus(request.status());
    entity.setFileGroupId(request.fileGroupId());
    entity.setNote(request.note());
    entity.setApprovalId(request.approvalId());  // ⭐ 추가
}
```

### **D. InspectionController.java**
```java
// src/main/java/com/cmms11/web/InspectionController.java

@PostMapping("/inspection/save")
public String saveForm(
    @ModelAttribute InspectionRequest request,
    @RequestParam(required = false) String isNew,
    @RequestParam(required = false) String action
) {
    if ("true".equals(isNew)) {
        service.create(request);
        return "redirect:/inspection/list";
    } else {
        service.update(request.inspectionId(), request);
        
        // 결재 상신: Approval 생성만 하고 data-redirect 처리
        if ("submit-approval".equals(action)) {
            service.saveDraftApproval(request.inspectionId());
        }
        
        // data-redirect가 처리 (JS)
        return "redirect:/inspection/detail/" + request.inspectionId();
    }
}
```

### **E. ApprovalService.java**
```java
// src/main/java/com/cmms11/approval/ApprovalService.java

@Autowired
private InspectionService inspectionService;

@Autowired
private WorkOrderService workOrderService;

@Autowired
private WorkPermitService workPermitService;

private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

public ApprovalResponse approve(String approvalId) {
    Approval entity = getExisting(approvalId);
    LocalDateTime now = LocalDateTime.now();
    String memberId = currentMemberId();

    // ⭐ 상태 체크 (APPROVING 포함)
    if (!"SUBMIT".equals(entity.getStatus()) && 
        !"APPROVING".equals(entity.getStatus())) {
        throw new IllegalStateException(
            "현재 상태에서는 승인할 수 없습니다. 현재 상태: " + entity.getStatus()
        );
    }
    
    entity.setStatus("COMPLETE");
    entity.setCompletedAt(now);
    entity.setUpdatedAt(now);
    entity.setUpdatedBy(memberId);
    
    Approval saved = repository.save(entity);
    notifyRefModule(saved, "COMPLETE");
    
    List<ApprovalStepResponse> steps = stepRepository
        .findByIdCompanyIdAndIdApprovalIdOrderByIdStepNo(
            MemberUserDetailsService.DEFAULT_COMPANY, approvalId)
        .stream()
        .map(ApprovalStepResponse::from)
        .collect(Collectors.toList());
    
    return ApprovalResponse.from(saved, steps);
}

public ApprovalResponse reject(String approvalId, String rejectReason) {
    Approval entity = getExisting(approvalId);
    
    // ⭐ 상태 체크 (APPROVING 포함)
    if (!"SUBMIT".equals(entity.getStatus()) && 
        !"APPROVING".equals(entity.getStatus())) {
        throw new IllegalStateException(
            "현재 상태에서는 반려할 수 없습니다. 현재 상태: " + entity.getStatus()
        );
    }
    
    entity.setStatus("REJECT");
    entity.setUpdatedAt(LocalDateTime.now());
    entity.setUpdatedBy(currentMemberId());
    
    Approval saved = repository.save(entity);
    notifyRefModule(saved, "REJECT");
    
    List<ApprovalStepResponse> steps = stepRepository
        .findByIdCompanyIdAndIdApprovalIdOrderByIdStepNo(
            MemberUserDetailsService.DEFAULT_COMPANY, approvalId)
        .stream()
        .map(ApprovalStepResponse::from)
        .collect(Collectors.toList());
    
    return ApprovalResponse.from(saved, steps);
}

/**
 * 원본 모듈에 상태 변경 통보
 */
private void notifyRefModule(Approval approval, String action) {
    if (approval.getRefEntity() == null || approval.getRefId() == null) {
        return;
    }
    
    try {
        switch (approval.getRefEntity()) {
            case "INSP":
                if ("COMPLETE".equals(action)) {
                    inspectionService.onApprovalComplete(approval.getRefId());
                } else if ("REJECT".equals(action)) {
                    inspectionService.onApprovalReject(approval.getRefId());
                }
                log.info("Inspection 모듈 콜백 완료: {} - {}", approval.getRefId(), action);
                break;
                
            case "WORK":
                if ("COMPLETE".equals(action)) {
                    workOrderService.onApprovalComplete(approval.getRefId());
                } else if ("REJECT".equals(action)) {
                    workOrderService.onApprovalReject(approval.getRefId());
                }
                log.info("WorkOrder 모듈 콜백 완료: {} - {}", approval.getRefId(), action);
                break;
                
            case "WPER":
                if ("COMPLETE".equals(action)) {
                    workPermitService.onApprovalComplete(approval.getRefId());
                } else if ("REJECT".equals(action)) {
                    workPermitService.onApprovalReject(approval.getRefId());
                }
                log.info("WorkPermit 모듈 콜백 완료: {} - {}", approval.getRefId(), action);
                break;
                
            default:
                log.warn("원본 모듈 콜백 대상이 아님: refEntity={}, refId={}", 
                    approval.getRefEntity(), approval.getRefId());
        }
    } catch (Exception e) {
        log.error("원본 모듈 콜백 실패: refEntity={}, refId={}, action={}", 
            approval.getRefEntity(), approval.getRefId(), action, e);
    }
}
```

---

## 3️⃣ Frontend 구현

### **A. inspection/form.html**
```html
<!-- src/main/resources/templates/inspection/form.html -->

<form data-validate 
      method="post" 
      th:action="@{/inspection/save}" 
      th:data-redirect="@{/inspection/detail/{id}(id=${inspection.inspectionId})}">
  
  <input type="hidden" name="_csrf" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />
  <input type="hidden" name="isNew" th:value="${isNew}" />
  
  <div class="card-header">
    <div class="card-title" th:text="${isNew ? '점검 등록' : '점검 수정'}">점검 수정</div>
    <div class="toolbar">
      <a class="btn" th:href="@{/inspection/list}">목록</a>
      
      <!-- 저장 버튼 -->
      <button class="btn" type="submit" name="action" value="save">
        <span th:text="${isNew ? '등록' : '저장'}">저장</span>
      </button>
      
      <!-- ⭐ 결재 상신 버튼 (수정 모드 && PROC 상태) -->
      <button th:if="${!isNew && inspection.status == 'PROC'}" 
              class="btn primary" 
              type="submit" 
              name="action" 
              value="submit-approval">
        결재 상신
      </button>
    </div>
  </div>
  
  <!-- 기존 폼 필드들 -->
</form>
```

### **B. inspection/detail.html**
```html
<!-- src/main/resources/templates/inspection/detail.html -->

<!doctype html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="utf-8" />
  <title>점검 상세</title>
  <link rel="stylesheet" href="../../static/assets/css/base.css" />
  
  <!-- ⭐ 팝업 모드 스타일 -->
  <style th:if="${param.popup != null}">
    body::before {
      content: '📄 점검 상세 정보';
      display: block;
      padding: 16px 20px;
      background: var(--primary);
      color: white;
      font-size: 18px;
      font-weight: 600;
      position: sticky;
      top: 0;
      z-index: 100;
    }
    .page { padding: 0; }
    .container { max-width: 100%; padding: 20px; }
  </style>
</head>
<body>
  <div class="page">
    <!-- ⭐ 팝업이 아닐 때만 표시 -->
    <header class="appbar" th:unless="${param.popup != null}">
      <div class="appbar-inner">
        <div class="brand">점검관리</div>
        <div class="spacer"></div>
        <div class="meta">
          <span class="badge">상세</span>
        </div>
      </div>
    </header>
    
    <nav class="breadcrumbs" th:unless="${param.popup != null}">
      <div class="container">
        <a th:href="@{/}">점검</a>
        <span class="sep">/</span>
        <a th:href="@{/inspection/list}">목록</a>
        <span class="sep">/</span>
        <span>상세</span>
      </div>
    </nav>
    
    <section data-slot-root th:fragment="content">
      <main>
        <div class="container">
          <section class="card">
            <div class="card-header">
              <div class="card-title">점검 상세</div>
              
              <!-- ⭐ 팝업이 아닐 때만 툴바 표시 -->
              <div class="toolbar" th:unless="${param.popup != null}">
                <a class="btn" th:href="@{/inspection/list}">목록</a>
                <a class="btn" th:href="@{/inspection/edit/{id}(id=${inspection.inspectionId})}">수정</a>
                <button class="btn danger" 
                        th:data-delete-url="@{/inspection/delete/{id}(id=${inspection.inspectionId})}"
                        data-redirect="@{/inspection/list}"
                        data-confirm="정말 삭제하시겠습니까?">삭제</button>
              </div>
            </div>
            
            <div class="card-body">
              <!-- 기존 섹션들 -->
              
              <!-- ⭐ 결재 정보 섹션 -->
              <div class="section" th:if="${inspection.approvalId != null}">
                <div class="section-title">결재 정보</div>
                <div class="grid cols-12">
                  <div class="stack col-span-3">
                    <div class="label">결재 번호</div>
                    <div>
                      <a th:href="@{/approval/detail/{id}(id=${inspection.approvalId})}" 
                         th:text="${inspection.approvalId}">A250101001</a>
                    </div>
                  </div>
                  <div class="stack col-span-3">
                    <div class="label">상태</div>
                    <div>
                      <span class="badge" 
                            th:text="${inspection.status == 'SUBMIT' ? '결재 중' : inspection.status == 'COMPLETE' ? '승인 완료' : inspection.status == 'REJECT' ? '반려' : inspection.status}"
                            th:classappend="${inspection.status == 'SUBMIT' ? 'warning' : inspection.status == 'COMPLETE' ? 'success' : inspection.status == 'REJECT' ? 'danger' : ''}">
                        결재 중
                      </span>
                    </div>
                  </div>
                  <div class="stack col-span-6">
                    <div class="label">액션</div>
                    <div>
                      <a class="btn btn-sm" 
                         th:href="@{/approval/edit/{id}(id=${inspection.approvalId})}"
                         th:if="${inspection.status == 'SUBMIT'}">
                        결재 수정
                      </a>
                      <a class="btn btn-sm" 
                         th:href="@{/approval/detail/{id}(id=${inspection.approvalId})}">
                        결재 확인
                      </a>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </div>
      </main>
    </section>
    
    <!-- ⭐ 팝업이 아닐 때만 표시 -->
    <footer th:unless="${param.popup != null}">
      <div class="container">© 점검관리</div>
    </footer>
  </div>
</body>
</html>
```

### **C. workorder/detail.html, workpermit/detail.html**
inspection/detail.html과 동일한 패턴으로 수정:
- `<header th:unless="${param.popup != null}">`
- `<nav th:unless="${param.popup != null}">`
- `<div class="toolbar" th:unless="${param.popup != null}">`
- `<footer th:unless="${param.popup != null}">`
- 팝업 모드 스타일 추가

### **D. approval/form.html**
```html
<!-- src/main/resources/templates/approval/form.html -->

<div class="form-row col-span-3">
  <label class="label" for="refId">참조 ID</label>
  <div style="display:flex; gap:8px;">
    <input id="refId" name="refId" class="input" type="text" 
           placeholder="예: O250101001" 
           th:value="${approval.refId}" 
           style="flex:1;" />
    
    <!-- ⭐ 팝업 버튼 -->
    <button type="button" 
            class="btn btn-sm" 
            onclick="openRefDocPopup()"
            th:disabled="${approval.refEntity == null || approval.refId == null}"
            title="원본 문서 보기">
      📄
    </button>
  </div>
</div>
```

### **E. approval/detail.html**
```html
<!-- src/main/resources/templates/approval/detail.html -->

<div class="stack col-span-3">
  <div class="label">참조 ID</div>
  <div style="display:flex; gap:8px; align-items:center;">
    <div th:text="${approval.refId ?: '-'}">I250101001</div>
    
    <!-- ⭐ 팝업 버튼 -->
    <button type="button" 
            class="btn btn-sm" 
            th:if="${approval.refEntity != null && approval.refId != null}"
            onclick="openRefDocPopup(this.dataset.module, this.dataset.id)"
            th:attr="data-module=${approval.refEntity}, data-id=${approval.refId}"
            title="원본 문서 보기">
      📄
    </button>
  </div>
</div>
```

### **F. approval.js**
```javascript
// src/main/resources/static/assets/js/pages/approval.js

// ⭐ 참조 문서 팝업 함수
window.openRefDocPopup = function(module, id) {
  if (!module || !id) {
    module = document.getElementById('refEntity')?.value;
    id = document.getElementById('refId')?.value;
  }
  
  if (!module || !id) {
    alert('참조 모듈과 ID를 입력하세요.');
    return;
  }
  
  const urls = {
    'INSP': `/inspection/detail/${id}?popup=true`,
    'WORK': `/workorder/detail/${id}?popup=true`,
    'WPER': `/workpermit/detail/${id}?popup=true`
  };
  
  if (!urls[module]) {
    alert('지원하지 않는 문서 타입입니다: ' + module);
    return;
  }
  
  const width = 1000;
  const height = 800;
  const left = (screen.width - width) / 2;
  const top = (screen.height - height) / 2;
  
  window.open(
    urls[module],
    '원본문서',
    `width=${width},height=${height},left=${left},top=${top},scrollbars=yes,resizable=yes`
  );
};
```

---

## 4️⃣ 상태 정의

### **Inspection 상태**
```
PLAN      - 계획
PROC      - 진행 중
SUBMIT    - 결재 상신
COMPLETE  - 완료
REJECT    - 반려
```

### **Approval 상태**
```
DRAFT      - 임시저장
SUBMIT     - 상신
APPROVING  - 결재 진행 중 (다단계 결재 시)
COMPLETE   - 완료
REJECT     - 반려
```

---

## 5️⃣ 수정 파일 체크리스트

### **Backend**
- [ ] `Inspection.java` - approvalId 필드
- [ ] `InspectionRequest.java` - approvalId 필드
- [ ] `InspectionService.java` - 3개 메서드 + applyRequest 수정
- [ ] `InspectionController.java` - action 처리
- [ ] `ApprovalService.java` - approve/reject/notifyRefModule (APPROVING 포함)

### **Frontend**
- [ ] `inspection/form.html` - 결재 상신 버튼
- [ ] `inspection/detail.html` - 팝업 모드 + 결재 정보 섹션
- [ ] `workorder/detail.html` - 팝업 모드
- [ ] `workpermit/detail.html` - 팝업 모드
- [ ] `approval/form.html` - 팝업 버튼
- [ ] `approval/detail.html` - 팝업 버튼
- [ ] `approval.js` - openRefDocPopup

### **Database**
- [ ] `migration_add_approval_link.sql`

---

## 6️⃣ 주요 변경 사항

| 항목 | 값 |
|------|------|
| **모듈 코드** | INSP, WORK, WPER |
| **Approval 상태** | DRAFT → SUBMIT → APPROVING → COMPLETE/REJECT |
| **기본 결재선** | 없음 (사용자 수동 입력) |
| **팝업 방식** | `?popup=true` 쿼리 파라미터 |
| **콜백 로그** | 성공/실패/대상 아님 모두 로그 |

---

**구현 준비 완료!**

