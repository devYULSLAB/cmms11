package com.cmms11.workorder;

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
 * 이름: WorkOrderApprovalFacade
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 작업지시 결재 워크플로우를 처리하는 Facade 서비스.
 *               WorkOrderService ↔ ApprovalService 순환 참조를 제거합니다.
 */
@Service
@Transactional
public class WorkOrderApprovalFacade {

    private final WorkOrderService workOrderService;
    private final ApprovalService approvalService;
    private final WorkOrderRepository repository;

    public WorkOrderApprovalFacade(
        WorkOrderService workOrderService,
        ApprovalService approvalService,
        WorkOrderRepository repository
    ) {
        this.workOrderService = workOrderService;
        this.approvalService = approvalService;
        this.repository = repository;
    }

    /**
     * 계획 결재 상신
     */
    public ApprovalResponse submitPlanApproval(String workOrderId) {
        WorkOrder order = getExisting(workOrderId);
        
        if (!"PLN".equals(order.getStage()) || !"DRAFT".equals(order.getStatus())) {
            throw new IllegalStateException("계획 작성 상태에서만 결재 요청 가능합니다.");
        }
        
        String content = buildPlanApprovalContent(order);
        ApprovalRequest request = new ApprovalRequest(
            null, "작업지시 계획 결재: " + order.getName(), "DRAFT",
            "WORK", workOrderId, "PLN", content, order.getFileGroupId(), new ArrayList<>()
        );
        
        ApprovalResponse approval = approvalService.create(request);
        order.setApprovalId(approval.approvalId());
        order.setStatus("SUBMT");
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(currentMemberId());
        repository.save(order);
        
        return approval;
    }

    /**
     * 실적 결재 상신
     */
    public ApprovalResponse submitActualApproval(String workOrderId) {
        WorkOrder order = getExisting(workOrderId);
        
        if (!"ACT".equals(order.getStage()) || !"DRAFT".equals(order.getStatus())) {
            throw new IllegalStateException("실적 작성 상태에서만 결재 요청 가능합니다.");
        }
        
        if (order.getActualDate() == null) {
            throw new IllegalStateException("실적 정보를 모두 입력해주세요.");
        }
        
        String content = buildActualApprovalContent(order);
        ApprovalRequest request = new ApprovalRequest(
            null, "작업지시 실적 결재: " + order.getName(), "DRAFT",
            "WORK", workOrderId, "ACT", content, order.getFileGroupId(), new ArrayList<>()
        );
        
        ApprovalResponse approval = approvalService.create(request);
        order.setApprovalId(approval.approvalId());
        order.setStatus("SUBMT");
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(currentMemberId());
        repository.save(order);
        
        return approval;
    }

    /**
     * 실적 입력 단계 준비
     */
    public void prepareActualStage(String workOrderId) {
        WorkOrder order = getExisting(workOrderId);
        
        if (!"PLN".equals(order.getStage()) || !"APPRV".equals(order.getStatus())) {
            throw new IllegalStateException("계획 결재가 완료되어야 실적을 입력할 수 있습니다.");
        }
        
        order.setStage("ACT");
        order.setStatus("DRAFT");
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(currentMemberId());
        repository.save(order);
    }

    // Helper methods
    private WorkOrder getExisting(String workOrderId) {
        return repository.findByIdCompanyIdAndIdOrderId(MemberUserDetailsService.DEFAULT_COMPANY, workOrderId)
            .orElseThrow(() -> new com.cmms11.common.error.NotFoundException("WorkOrder not found: " + workOrderId));
    }

    private String currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }

    private String buildPlanApprovalContent(WorkOrder order) {
        return "<h3>작업지시 계획 결재 요청</h3><p>작업지시 번호: " + order.getId().getOrderId() + "</p>";
    }

    private String buildActualApprovalContent(WorkOrder order) {
        return "<h3>작업지시 실적 결재 요청</h3><p>작업지시 번호: " + order.getId().getOrderId() + "</p>";
    }
}

