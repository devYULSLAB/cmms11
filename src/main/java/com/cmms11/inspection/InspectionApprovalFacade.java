package com.cmms11.inspection;

import com.cmms11.approval.ApprovalRequest;
import com.cmms11.approval.ApprovalResponse;
import com.cmms11.approval.ApprovalService;
import com.cmms11.security.MemberUserDetailsService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이름: InspectionApprovalFacade
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 예방점검 결재 워크플로우를 처리하는 Facade 서비스.
 *               InspectionService ↔ ApprovalService 순환 참조를 제거합니다.
 */
@Service
@Transactional
public class InspectionApprovalFacade {

    private final InspectionService inspectionService;
    private final ApprovalService approvalService;
    private final InspectionRepository repository;

    public InspectionApprovalFacade(
        InspectionService inspectionService,
        ApprovalService approvalService,
        InspectionRepository repository
    ) {
        this.inspectionService = inspectionService;
        this.approvalService = approvalService;
        this.repository = repository;
    }

    /**
     * 계획 결재 상신
     */
    public ApprovalResponse submitPlanApproval(String inspectionId) {
        Inspection inspection = getExisting(inspectionId);
        
        // 상태 검증
        if (!"PLN".equals(inspection.getStage()) || !"DRAFT".equals(inspection.getStatus())) {
            throw new IllegalStateException("작성 중인 계획만 결재 요청 가능합니다. 현재 단계/상태: " + inspection.getStage() + "/" + inspection.getStatus());
        }
        
        // 결재 본문 자동 생성
        String content = buildPlanApprovalContent(inspection);
        
        // 빈 결재선으로 Approval 생성
        ApprovalRequest request = new ApprovalRequest(
            null,           // approvalId
            "점검 계획 결재: " + inspection.getName(),  // title
            "DRAFT",        // status
            "INSP",         // refEntity
            inspectionId,   // refId
            "PLN",          // refStage
            content,        // content
            inspection.getFileGroupId(),  // fileGroupId
            new ArrayList<>()  // steps
        );
        
        ApprovalResponse approval = approvalService.create(request);
        
        // Inspection 업데이트
        inspection.setApprovalId(approval.approvalId());
        inspection.setStatus("SUBMT");
        inspection.setUpdatedAt(LocalDateTime.now());
        inspection.setUpdatedBy(currentMemberId());
        repository.save(inspection);
        
        return approval;
    }

    /**
     * 실적 결재 상신
     */
    public ApprovalResponse submitActualApproval(String inspectionId) {
        Inspection inspection = getExisting(inspectionId);
        
        // 상태 검증
        if (!"ACT".equals(inspection.getStage()) || !"DRAFT".equals(inspection.getStatus())) {
            throw new IllegalStateException("작성 중인 점검만 결재 요청 가능합니다. 현재 단계/상태: " + inspection.getStage() + "/" + inspection.getStatus());
        }
        
        // 결재 본문 자동 생성
        String content = buildActualApprovalContent(inspection);
        
        // 빈 결재선으로 Approval 생성
        ApprovalRequest request = new ApprovalRequest(
            null,           // approvalId
            "점검 결재: " + inspection.getName(),  // title
            "DRAFT",        // status
            "INSP",         // refEntity
            inspectionId,   // refId
            "ACT",          // refStage
            content,        // content
            inspection.getFileGroupId(),  // fileGroupId
            new ArrayList<>()  // steps
        );
        
        ApprovalResponse approval = approvalService.create(request);
        
        // Inspection 업데이트
        inspection.setApprovalId(approval.approvalId());
        inspection.setStatus("SUBMT");
        inspection.setUpdatedAt(LocalDateTime.now());
        inspection.setUpdatedBy(currentMemberId());
        repository.save(inspection);
        
        return approval;
    }

    /**
     * 실적 입력 단계 준비 (PLN+APPRV → ACT+DRAFT)
     */
    public void prepareActualStage(String inspectionId) {
        Inspection inspection = getExisting(inspectionId);
        
        // 상태 검증
        if (!"PLN".equals(inspection.getStage()) || !"APPRV".equals(inspection.getStatus())) {
            throw new IllegalStateException(
                "계획이 확정되어야 실적을 입력할 수 있습니다. 현재 단계/상태: " + 
                inspection.getStage() + "/" + inspection.getStatus()
            );
        }
        
        // 상태 전환
        inspection.setStage("ACT");
        inspection.setStatus("DRAFT");
        inspection.setUpdatedAt(LocalDateTime.now());
        inspection.setUpdatedBy(currentMemberId());
        repository.save(inspection);
    }

    // ========== Helper Methods ==========

    private Inspection getExisting(String inspectionId) {
        return repository
            .findByIdCompanyIdAndIdInspectionId(MemberUserDetailsService.DEFAULT_COMPANY, inspectionId)
            .orElseThrow(() -> new com.cmms11.common.error.NotFoundException("Inspection not found: " + inspectionId));
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
     * 계획 결재 문서 생성
     */
    private String buildPlanApprovalContent(Inspection inspection) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>점검 계획 결재 요청</h3>");
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
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getPlannedDate() != null ? inspection.getPlannedDate().toString() : "-").append("</td></tr>");
        
        sb.append("</table>");
        
        if (inspection.getNote() != null && !inspection.getNote().isEmpty()) {
            sb.append("<p style='margin-top:15px;'><strong>비고:</strong></p>");
            sb.append("<p>").append(inspection.getNote()).append("</p>");
        }
        
        return sb.toString();
    }

    /**
     * 실적 결재 본문 생성
     */
    private String buildActualApprovalContent(Inspection inspection) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>점검 실적 결재 요청</h3>");
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
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getPlannedDate() != null ? inspection.getPlannedDate().toString() : "-").append("</td></tr>");
        
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>실적일</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getActualDate() != null ? inspection.getActualDate().toString() : "-").append("</td></tr>");
        
        sb.append("</table>");
        
        if (inspection.getNote() != null && !inspection.getNote().isEmpty()) {
            sb.append("<p style='margin-top:15px;'><strong>비고:</strong></p>");
            sb.append("<p>").append(inspection.getNote()).append("</p>");
        }
        
        return sb.toString();
    }
}

