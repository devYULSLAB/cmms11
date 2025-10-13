package com.cmms11.workpermit;

import com.cmms11.approval.ApprovalRequest;
import com.cmms11.approval.ApprovalResponse;
import com.cmms11.approval.ApprovalService;
import com.cmms11.common.error.NotFoundException;
import com.cmms11.common.seq.AutoNumberService;
import com.cmms11.security.MemberUserDetailsService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이름: WorkPermitService
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 작업허가 트랜잭션 CRUD 로직을 처리하는 서비스.
 */
@Service
@Transactional
public class WorkPermitService {

    private static final String MODULE_CODE = "P";

    private final WorkPermitRepository repository;
    private final WorkPermitItemRepository itemRepository;
    private final AutoNumberService autoNumberService;
    
    @Autowired
    private ApprovalService approvalService;

    public WorkPermitService(WorkPermitRepository repository, AutoNumberService autoNumberService, WorkPermitItemRepository itemRepository) {
        this.repository = repository;
        this.autoNumberService = autoNumberService;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public Page<WorkPermitResponse> list(String permitId, String plantId, String jobId, String status, String stage, String plannedDateFrom, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        
        // 날짜 문자열을 LocalDate로 변환
        java.time.LocalDate fromDate = null;
        
        try {
            if (plannedDateFrom != null && !plannedDateFrom.isBlank()) {
                fromDate = java.time.LocalDate.parse(plannedDateFrom);
            }
        } catch (java.time.format.DateTimeParseException e) {
            // 날짜 파싱 오류 시 무시
        }
        
        Page<WorkPermit> page = repository.findByFilters(
            companyId, 
            permitId, 
            plantId, 
            jobId, 
            status, 
            stage,
            fromDate, 
            pageable
        );
        
        return page.map(WorkPermitResponse::from);
    }

    @Transactional(readOnly = true)
    public WorkPermitResponse get(String permitId) {
        return WorkPermitResponse.from(getExisting(permitId));
    }

    @Transactional(readOnly = true)
    public java.util.List<WorkPermitItem> getItems(String permitId) {
        return itemRepository.findByPermit(com.cmms11.security.MemberUserDetailsService.DEFAULT_COMPANY, permitId);
    }

    public WorkPermitResponse create(WorkPermitRequest request) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        LocalDateTime now = LocalDateTime.now();
        String memberId = currentMemberId();

        String newId = resolveId(companyId, request.permitId(), request.plannedDate());
        WorkPermit entity = new WorkPermit();
        entity.setId(new WorkPermitId(companyId, newId));
        entity.setCreatedAt(now);
        entity.setCreatedBy(memberId);
        applyRequest(entity, request);
        // ⭐ 신규 생성 시 초기 상태 설정 (작업허가는 PLN 만 존재:ACT 없음)
        entity.setStage("PLN");
        entity.setStatus("DRAFT");

        entity.setUpdatedAt(now);
        entity.setUpdatedBy(memberId);

        WorkPermit saved = repository.save(entity);
        saveItems(saved.getId().getCompanyId(), saved.getId().getPermitId(), request);
        return WorkPermitResponse.from(saved);
    }

    public WorkPermitResponse update(String permitId, WorkPermitRequest request) {
        WorkPermit entity = getExisting(permitId);
        applyRequest(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentMemberId());
        WorkPermit saved = repository.save(entity);
        saveItems(saved.getId().getCompanyId(), saved.getId().getPermitId(), request);
        return WorkPermitResponse.from(saved);
    }

    public void delete(String permitId) {
        WorkPermit entity = getExisting(permitId);
        repository.delete(entity);
    }

    private WorkPermit getExisting(String permitId) {
        return repository
            .findByIdCompanyIdAndIdPermitId(MemberUserDetailsService.DEFAULT_COMPANY, permitId)
            .orElseThrow(() -> new NotFoundException("WorkPermit not found: " + permitId));
    }

    private void applyRequest(WorkPermit entity, WorkPermitRequest request) {
        entity.setName(request.name());
        entity.setPlantId(request.plantId());
        entity.setJobId(request.jobId());
        entity.setSiteId(request.siteId());
        entity.setDeptId(request.deptId());
        entity.setMemberId(request.memberId());
        entity.setPlannedDate(request.plannedDate());
        entity.setActualDate(request.actualDate());
        entity.setWorkSummary(request.workSummary());
        entity.setHazardFactor(request.hazardFactor());
        entity.setSafetyFactor(request.safetyFactor());
        entity.setChecksheetJson(request.checksheetJson());
        // ⭐ status/stage는 사용자 입력으로 변경 불가 (워크플로우로만 변경)
        // entity.setStatus(request.status());
        // entity.setStage(request.stage());
        
        entity.setRefEntity(request.refEntity());
        entity.setRefId(request.refId());
        entity.setRefStage(request.refStage());
        entity.setFileGroupId(request.fileGroupId());
        entity.setNote(request.note());
    }

    private void saveItems(String companyId, String permitId, WorkPermitRequest request) {
        // replace-all strategy for simplicity
        itemRepository.deleteByPermit(companyId, permitId);
        if (request.items() == null || request.items().isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        String memberId = currentMemberId();
        int line = 1;
        for (WorkPermitItemRequest r : request.items()) {
            if (r == null) continue;
            WorkPermitItem e = new WorkPermitItem();
            e.setId(new WorkPermitItemId(companyId, permitId, line++));
            e.setName(r.name());
            e.setSignature(r.signature());
            e.setNote(r.note());
            e.setCreatedAt(now);
            e.setCreatedBy(memberId);
            e.setUpdatedAt(now);
            e.setUpdatedBy(memberId);
            itemRepository.save(e);
        }
    }

    private String resolveId(String companyId, String requestedId, LocalDate referenceDate) {
        if (requestedId != null && !requestedId.isBlank()) {
            String trimmed = requestedId.trim();
            repository
                .findByIdCompanyIdAndIdPermitId(companyId, trimmed)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("WorkPermit already exists: " + trimmed);
                });
            return trimmed;
        }
        LocalDate date = referenceDate != null ? referenceDate : LocalDate.now();
        return autoNumberService.generateTxId(companyId, MODULE_CODE, date);
    }

    private String currentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }
        String name = authentication.getName();
        return name != null ? name : "system";
    }

    /**
     * 결재 상신 
     */
    public ApprovalResponse submitPlanApproval(String permitId) {
        WorkPermit permit = getExisting(permitId);
        
        // PLN,DRAFT 상태에서만 결재 상신 가능 (반려 시 이미 PLN_DRAFT로 복원됨)
        if (!"PLN".equals(permit.getStage()) || !"DRAFT".equals(permit.getStatus())) {
            throw new IllegalStateException("작성 중인 작업허가만 결재 요청 가능합니다. 현재 상태: " + permit.getStatus());
        }
        
        // 결재 본문 자동 생성
        String content = buildPlanApprovalContent(permit);
        
        // 빈 결재선으로 Approval 생성
        ApprovalRequest request = new ApprovalRequest(
            null,           // approvalId
            "작업허가 결재: " + permit.getName(),  // title
            "DRAFT",        // status
            "WPER",         // refEntity
            permitId,       // refId
            "PLN",          // refStage
            content,        // content
            permit.getFileGroupId(),  // fileGroupId
            new ArrayList<>()  // steps
        );
        
        ApprovalResponse approval = approvalService.create(request);
        
        // WorkPermit 업데이트 및 approvalId 저장
        permit.setApprovalId(approval.approvalId());
        permit.setStatus("SUBMT");
        permit.setUpdatedAt(LocalDateTime.now());
        permit.setUpdatedBy(currentMemberId());
        repository.save(permit);
        
        return approval;
    }

    /**
     * 계획 결재 승인 콜백
     */
    public void onPlanApprovalApprove(String permitId) {
        WorkPermit permit = getExisting(permitId);
        permit.setStatus("APPRV");
        permit.setUpdatedAt(LocalDateTime.now());
        permit.setUpdatedBy(currentMemberId());
        repository.save(permit);
    }

    /**
     * 계획 자체 확정 (결재 없이 DRAFT → CMPLT)
     */
    public void onPlanApprovalComplete(String permitId) {
        WorkPermit permit = getExisting(permitId);
        if (!"PLN".equals(permit.getStage()) || !"DRAFT".equals(permit.getStatus())) {
            throw new IllegalStateException("작성 중인 계획만 확정 가능합니다.");
        }
        permit.setStatus("CMPLT");
        permit.setUpdatedAt(LocalDateTime.now());
        permit.setUpdatedBy(currentMemberId());
        repository.save(permit);
    }

    /**
     * 계획 결재 반려 콜백
     */
    public void onPlanApprovalReject(String permitId) {
        WorkPermit permit = getExisting(permitId);
        permit.setStatus("REJCT");
        permit.setUpdatedAt(LocalDateTime.now());
        permit.setUpdatedBy(currentMemberId());
        repository.save(permit);
    }

    /**
     * 계획 결재 삭제 콜백
     */
    public void onPlanApprovalDelete(String permitId) {
        WorkPermit permit = getExisting(permitId);
        permit.setStatus("DRAFT");
        permit.setApprovalId(null);
        permit.setUpdatedAt(LocalDateTime.now());
        permit.setUpdatedBy(currentMemberId());
        repository.save(permit);
    }

    /**
     * @deprecated Use {@link #onPlanApprovalApprove(String)} instead
     */
    @Deprecated
    public void onApprovalComplete(String permitId) {
        onPlanApprovalApprove(permitId);
    }

    /**
     * @deprecated Use {@link #onPlanApprovalReject(String)} instead
     */
    @Deprecated
    public void onApprovalReject(String permitId) {
        onPlanApprovalReject(permitId);
    }

    /**
     * @deprecated Use {@link #onPlanApprovalDelete(String)} instead
     */
    @Deprecated
    public void onApprovalDelete(String permitId) {
        onPlanApprovalDelete(permitId);
    }

    /**
     * 계획 결재 본문 생성
     */
    private String buildPlanApprovalContent(WorkPermit permit) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>작업허가 결재 요청</h3>");
        sb.append("<table style='border-collapse:collapse; width:100%;'>");
        sb.append("<tr><th style='border:1px solid #ddd; padding:8px; background:#f5f5f5;'>항목</th>");
        sb.append("<th style='border:1px solid #ddd; padding:8px; background:#f5f5f5;'>내용</th></tr>");
        
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>허가 번호</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(permit.getId().getPermitId()).append("</td></tr>");
        
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>작업명</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(permit.getName()).append("</td></tr>");
        
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>설비</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(permit.getPlantId() != null ? permit.getPlantId() : "-").append("</td></tr>");
        
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>담당자</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(permit.getMemberId() != null ? permit.getMemberId() : "-").append("</td></tr>");
        
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>계획일</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(permit.getPlannedDate() != null ? permit.getPlannedDate().toString() : "-").append("</td></tr>");
        
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>실적일</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(permit.getActualDate() != null ? permit.getActualDate().toString() : "-").append("</td></tr>");
        
        sb.append("</table>");
        
        if (permit.getWorkSummary() != null && !permit.getWorkSummary().isEmpty()) {
            sb.append("<p style='margin-top:15px;'><strong>작업 개요:</strong></p>");
            sb.append("<p>").append(permit.getWorkSummary()).append("</p>");
        }
        
        if (permit.getHazardFactor() != null && !permit.getHazardFactor().isEmpty()) {
            sb.append("<p style='margin-top:15px;'><strong>위험 요인:</strong></p>");
            sb.append("<p>").append(permit.getHazardFactor()).append("</p>");
        }
        
        if (permit.getSafetyFactor() != null && !permit.getSafetyFactor().isEmpty()) {
            sb.append("<p style='margin-top:15px;'><strong>안전 조치:</strong></p>");
            sb.append("<p>").append(permit.getSafetyFactor()).append("</p>");
        }
        
        if (permit.getNote() != null && !permit.getNote().isEmpty()) {
            sb.append("<p style='margin-top:15px;'><strong>비고:</strong></p>");
            sb.append("<p>").append(permit.getNote()).append("</p>");
        }
        
        return sb.toString();
    }
}
