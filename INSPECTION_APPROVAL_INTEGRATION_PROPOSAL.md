# Inspection-Approval 통합 제안서

## 1. 현황 분석

### 기존 Approval 모듈 구조
```java
// Approval 엔티티
- refEntity: 참조 모듈 코드 (예: "INSP", "WO", "WP")
- refId: 참조 ID (예: "I250101001")
- status: 결재 상태 (DRAFT, SUBMIT, APPROVAL, COMPLETE, REJECT)
```

### 기존 Inspection 모듈 상태
```java
- PLAN: 계획 (plan.html에서 생성)
- PROC: 진행 (form.html에서 수정)
- DONE: 완료
```

## 2. 제안 워크플로우

### 상태 흐름
```
[계획 수립]           [담당자 작업]         [결재 요청]          [결재 완료]
plan.html  ─────>  form.html  ─────>  결재상신  ─────>  approval
   │                   │                  │               │
status=PLAN      status=PROC         approval.status  status=DONE
                                     =SUBMIT/APPROVAL  (결재완료 시)
                 (담당자 배정,
                  세부항목,
                  결과값 입력)
```

## 3. 구현 방안

### 3-1. Inspection 엔티티 확장
```java
// Inspection.java에 결재 연동 필드 추가
@Column(name = "approval_id", length = 10)
private String approvalId;  // 연결된 결재 ID
```

### 3-2. InspectionRequest DTO 확장
```java
// InspectionRequest.java
public record InspectionRequest(
    // ... 기존 필드들
    @Size(max = 10) String approvalId  // 추가
) {}
```

### 3-3. InspectionService 메서드 추가
```java
/**
 * 결재 상신 (PROC → 결재 중)
 */
public ApprovalResponse submitForApproval(String inspectionId) {
    Inspection inspection = getExisting(inspectionId);
    
    // 상태 검증
    if (!"PROC".equals(inspection.getStatus())) {
        throw new IllegalStateException("진행 중인 점검만 결재 요청 가능합니다.");
    }
    
    // Approval 생성
    ApprovalRequest approvalRequest = new ApprovalRequest(
        null, // 자동 생성
        "점검 결재: " + inspection.getName(),
        "SUBMIT",
        "INSP", // 참조 모듈
        inspectionId, // 참조 ID
        buildApprovalContent(inspection),
        inspection.getFileGroupId(),
        getDefaultApprovalSteps() // 결재선 기본값
    );
    
    ApprovalResponse approval = approvalService.create(approvalRequest);
    
    // inspection에 approvalId 연결
    inspection.setApprovalId(approval.approvalId());
    inspection.setUpdatedAt(LocalDateTime.now());
    inspection.setUpdatedBy(currentMemberId());
    repository.save(inspection);
    
    return approval;
}

/**
 * 결재 완료 콜백 (APPROVAL COMPLETE → DONE)
 */
public void onApprovalComplete(String inspectionId) {
    Inspection inspection = getExisting(inspectionId);
    inspection.setStatus("DONE");
    inspection.setUpdatedAt(LocalDateTime.now());
    inspection.setUpdatedBy(currentMemberId());
    repository.save(inspection);
}

/**
 * 결재 반려 콜백 (REJECT → PROC)
 */
public void onApprovalReject(String inspectionId) {
    Inspection inspection = getExisting(inspectionId);
    inspection.setStatus("PROC");
    inspection.setApprovalId(null); // 결재 연결 해제
    inspection.setUpdatedAt(LocalDateTime.now());
    inspection.setUpdatedBy(currentMemberId());
    repository.save(inspection);
}

private String buildApprovalContent(Inspection inspection) {
    return String.format(
        "점검명: %s\n설비: %s\n담당자: %s\n계획일: %s\n실적일: %s",
        inspection.getName(),
        inspection.getPlantId(),
        inspection.getMemberId(),
        inspection.getPlannedDate(),
        inspection.getActualDate()
    );
}

private List<ApprovalStepRequest> getDefaultApprovalSteps() {
    // 기본 결재선 설정 (팀장 → 부서장)
    return List.of(
        new ApprovalStepRequest(1, "M0001", null, null, null),
        new ApprovalStepRequest(2, "M0002", null, null, null)
    );
}
```

### 3-4. ApprovalService 콜백 추가
```java
/**
 * 결재 승인 시 원본 모듈 업데이트
 */
public ApprovalResponse approve(String approvalId) {
    Approval entity = getExisting(approvalId);
    LocalDateTime now = LocalDateTime.now();
    String memberId = currentMemberId();

    // 결재 상태 업데이트
    if ("SUBMIT".equals(entity.getStatus()) || "APPROVAL".equals(entity.getStatus())) {
        entity.setStatus("COMPLETE");
        entity.setCompletedAt(now);
    }
    entity.setUpdatedAt(now);
    entity.setUpdatedBy(memberId);
    
    Approval saved = repository.save(entity);
    
    // 원본 모듈 콜백 처리
    notifyRefModule(saved, "COMPLETE");
    
    List<ApprovalStepResponse> steps = stepRepository
        .findByIdCompanyIdAndIdApprovalIdOrderByIdStepNo(
            MemberUserDetailsService.DEFAULT_COMPANY, approvalId)
        .stream()
        .map(ApprovalStepResponse::from)
        .collect(Collectors.toList());
    return ApprovalResponse.from(saved, steps);
}

/**
 * 결재 반려 시 원본 모듈 업데이트
 */
public ApprovalResponse reject(String approvalId, String rejectReason) {
    Approval entity = getExisting(approvalId);
    entity.setStatus("REJECT");
    entity.setUpdatedAt(LocalDateTime.now());
    entity.setUpdatedBy(currentMemberId());
    
    Approval saved = repository.save(entity);
    
    // 원본 모듈 콜백 처리
    notifyRefModule(saved, "REJECT");
    
    // ... (생략)
}

/**
 * 참조 모듈에 상태 변경 통보
 */
private void notifyRefModule(Approval approval, String approvalStatus) {
    if (approval.getRefEntity() == null || approval.getRefId() == null) {
        return;
    }
    
    switch (approval.getRefEntity()) {
        case "INSP":
            if ("COMPLETE".equals(approvalStatus)) {
                inspectionService.onApprovalComplete(approval.getRefId());
            } else if ("REJECT".equals(approvalStatus)) {
                inspectionService.onApprovalReject(approval.getRefId());
            }
            break;
        case "WO":
            // 작업지시 콜백
            break;
        case "WP":
            // 작업허가 콜백
            break;
        // ... 다른 모듈
    }
}
```

### 3-5. InspectionController 엔드포인트 추가
```java
/**
 * 결재 상신
 */
@PostMapping("/inspection/{inspectionId}/submit-approval")
public String submitApproval(@PathVariable String inspectionId) {
    ApprovalResponse approval = service.submitForApproval(inspectionId);
    return "redirect:/approval/detail/" + approval.approvalId();
}

/**
 * API: 결재 상신
 */
@ResponseBody
@PostMapping("/api/inspections/{inspectionId}/submit-approval")
public ResponseEntity<ApprovalResponse> submitApprovalApi(@PathVariable String inspectionId) {
    ApprovalResponse approval = service.submitForApproval(inspectionId);
    return ResponseEntity.ok(approval);
}
```

### 3-6. detail.html에 결재 상신 버튼 추가
```html
<!-- inspection/detail.html -->
<div class="card-header">
  <div class="card-title">점검 상세</div>
  <div class="toolbar">
    <a class="btn" th:href="@{/inspection/list}">목록</a>
    <a class="btn" th:href="@{/inspection/edit/{id}(id=${inspection.inspectionId})}">수정</a>
    
    <!-- 결재 상신 버튼 (진행 중 상태일 때만 표시) -->
    <form th:if="${inspection.status == 'PROC' && inspection.approvalId == null}" 
          method="post" 
          th:action="@{/inspection/{id}/submit-approval(id=${inspection.inspectionId})}"
          style="display:inline;">
      <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />
      <button class="btn primary" type="submit">결재 상신</button>
    </form>
    
    <!-- 결재 진행 중 표시 -->
    <a th:if="${inspection.approvalId != null}" 
       class="btn warning" 
       th:href="@{/approval/detail/{id}(id=${inspection.approvalId})}">
      결재 확인
    </a>
    
    <button class="btn danger" 
            th:data-delete-url="@{/inspection/delete/{id}(id=${inspection.inspectionId})}"
            data-redirect="/inspection/list"
            data-confirm="정말로 삭제하시겠습니까?">
      삭제
    </button>
  </div>
</div>

<!-- 결재 정보 섹션 추가 -->
<div class="section" th:if="${inspection.approvalId != null}">
  <div class="section-title">결재 정보</div>
  <div class="grid cols-12">
    <div class="stack col-span-12">
      <div class="label">결재 문서</div>
      <div>
        <a th:href="@{/approval/detail/{id}(id=${inspection.approvalId})}" 
           th:text="${inspection.approvalId}">A250101001</a>
      </div>
    </div>
  </div>
</div>
```

## 4. 데이터베이스 마이그레이션

```sql
-- Inspection 테이블에 approval_id 컬럼 추가
ALTER TABLE inspection 
ADD COLUMN approval_id CHAR(10) AFTER status;

-- 인덱스 추가 (결재 ID로 점검 조회)
CREATE INDEX ix_inspection_approval ON inspection(company_id, approval_id);
```

## 5. 상태 변경 시나리오

### 시나리오 1: 정상 승인
```
1. plan.html에서 점검 생성 (status=PLAN)
2. list에서 수정 버튼 클릭
3. form.html에서 담당자 배정, 항목 입력 (status=PROC로 변경)
4. detail에서 "결재 상신" 버튼 클릭
   - Approval 레코드 생성 (status=SUBMIT, refEntity=INSP, refId=inspectionId)
   - Inspection.approvalId 업데이트
5. 결재자가 승인
   - Approval.status = COMPLETE
   - Inspection.status = DONE (자동)
```

### 시나리오 2: 반려
```
1~4. 동일
5. 결재자가 반려
   - Approval.status = REJECT
   - Inspection.status = PROC (재작업)
   - Inspection.approvalId = null (연결 해제)
6. 담당자가 수정 후 재상신
```

## 6. 장점

### 기존 구조 최대한 활용
- Approval의 refEntity/refId 구조 그대로 사용
- 최소한의 컬럼 추가 (inspection.approval_id만)

### 명확한 상태 관리
- PLAN: 계획만 수립
- PROC: 담당자 작업 중
- DONE: 결재 완료

### 느슨한 결합
- Inspection은 Approval에 의존하지 않음
- Approval 모듈만 Inspection 콜백 호출

### 확장 가능
- WorkOrder, WorkPermit 등 다른 모듈도 동일 패턴 적용 가능

## 7. 구현 순서

1. **DB 마이그레이션**: inspection 테이블에 approval_id 추가
2. **Entity/DTO 확장**: Inspection, InspectionRequest에 approvalId 추가
3. **Service 메서드**: submitForApproval, onApprovalComplete, onApprovalReject
4. **ApprovalService 콜백**: notifyRefModule 구현
5. **Controller 엔드포인트**: 결재 상신 API 추가
6. **UI 수정**: detail.html에 결재 상신 버튼 추가
7. **테스트**: 전체 워크플로우 검증

## 8. 주의사항

### 결재선 설정
- 현재는 하드코딩된 기본 결재선 사용
- 향후 부서별/직급별 결재선 설정 기능 추가 고려

### 권한 관리
- 담당자만 결재 상신 가능하도록 권한 체크 필요
- 결재 완료 후 수정 불가 처리

### 트랜잭션 관리
- Approval 생성과 Inspection 업데이트는 하나의 트랜잭션으로 처리
- 콜백 실패 시 롤백 전략 수립

## 9. 향후 확장

### Phase 2
- 결재선 동적 설정 (조직도 기반)
- 결재 위임/대결 기능
- 결재 알림 (메일/푸시)

### Phase 3
- 조건부 결재 (금액별, 중요도별)
- 병렬 결재 (AND/OR 결재)
- 결재 이력 상세 보기

