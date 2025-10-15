package com.cmms11.workpermit;

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
 * 이름: WorkPermitApprovalFacade
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 작업허가 결재 워크플로우를 처리하는 Facade 서비스.
 *               WorkPermitService ↔ ApprovalService 순환 참조를 제거합니다.
 */
@Service
@Transactional
public class WorkPermitApprovalFacade {

    private final WorkPermitService workPermitService;
    private final ApprovalService approvalService;
    private final WorkPermitRepository repository;

    public WorkPermitApprovalFacade(
        WorkPermitService workPermitService,
        ApprovalService approvalService,
        WorkPermitRepository repository
    ) {
        this.workPermitService = workPermitService;
        this.approvalService = approvalService;
        this.repository = repository;
    }

    /**
     * 계획 결재 상신 (WorkPermit은 PLN만 있음)
     */
    public ApprovalResponse submitPlanApproval(String permitId) {
        WorkPermit permit = getExisting(permitId);
        
        if (!"PLN".equals(permit.getStage()) || !"DRAFT".equals(permit.getStatus())) {
            throw new IllegalStateException("작성 중인 허가만 결재 요청 가능합니다.");
        }
        
        String content = buildPlanApprovalContent(permit);
        ApprovalRequest request = new ApprovalRequest(
            null, "작업허가 결재: " + permit.getName(), "DRAFT",
            "WPER", permitId, "PLN", content, permit.getFileGroupId(), new ArrayList<>()
        );
        
        ApprovalResponse approval = approvalService.create(request);
        permit.setApprovalId(approval.approvalId());
        permit.setStatus("SUBMT");
        permit.setUpdatedAt(LocalDateTime.now());
        permit.setUpdatedBy(currentMemberId());
        repository.save(permit);
        
        return approval;
    }

    // Helper methods
    private WorkPermit getExisting(String permitId) {
        return repository.findByIdCompanyIdAndIdPermitId(MemberUserDetailsService.DEFAULT_COMPANY, permitId)
            .orElseThrow(() -> new com.cmms11.common.error.NotFoundException("WorkPermit not found: " + permitId));
    }

    private String currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }

    private String buildPlanApprovalContent(WorkPermit permit) {
        return "<h3>작업허가 결재 요청</h3><p>허가 번호: " + permit.getId().getPermitId() + "</p>";
    }
}

