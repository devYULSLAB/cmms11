package com.cmms11.workorder;

import com.cmms11.approval.ApprovalRequest;
import com.cmms11.approval.ApprovalResponse;
import com.cmms11.approval.ApprovalStepRequest;
import com.cmms11.approval.client.ApprovalClient;
import com.cmms11.approval.client.ApprovalLineStepRequest;
import com.cmms11.approval.client.ApprovalStatusTransition;
import com.cmms11.approval.client.ApprovalSubmissionRequest;
import com.cmms11.config.ApprovalWebhookProperties;
import com.cmms11.security.MemberUserDetailsService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkOrderApprovalService {

    private static final String REF_ENTITY = "WORK";
    private static final String PLAN_STAGE = "PLN";
    private static final String ACT_STAGE = "ACT";
    private static final String CALLBACK_PATH = "/api/work-orders/approvals/webhook";
    private static final String STATUS_SUBMITTED = "SUBMT";
    private static final String SYSTEM_ACTOR = "system";

    private final WorkOrderRepository repository;
    private final ApprovalClient approvalClient;
    private final ApprovalWebhookProperties webhookProperties;

    public WorkOrderApprovalService(
        WorkOrderRepository repository,
        ApprovalClient approvalClient,
        ApprovalWebhookProperties webhookProperties
    ) {
        this.repository = repository;
        this.approvalClient = approvalClient;
        this.webhookProperties = webhookProperties;
    }

    @Transactional
    public ApprovalResponse submitApproval(String workOrderId, ApprovalSubmissionRequest request) {
        WorkOrder order = repository
            .findByIdCompanyIdAndIdOrderId(MemberUserDetailsService.DEFAULT_COMPANY, workOrderId)
            .orElseThrow(() -> new com.cmms11.common.error.NotFoundException("WorkOrder not found: " + workOrderId));

        String stage = normalizeStage(request.stage(), order.getStage());
        validateStatusForSubmission(order, stage);

        ApprovalRequest approvalRequest = buildApprovalRequest(order, stage, request);
        ApprovalResponse approval = approvalClient.submitApproval(approvalRequest);

        order.setApprovalId(approval.approvalId());
        order.setStage(stage);
        order.setStatus(STATUS_SUBMITTED);
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(MemberUserDetailsService.getCurrentMemberId());
        repository.save(order);

        return approval;
    }

    private void validateStatusForSubmission(WorkOrder order, String stage) {
        if (!"DRAFT".equals(order.getStatus())) {
            throw new IllegalStateException("작성 중(DRAFT) 문서만 결재 상신할 수 있습니다.");
        }
        if (ACT_STAGE.equals(stage) && order.getActualDate() == null) {
            throw new IllegalStateException("실적 정보를 모두 입력해주세요.");
        }
    }

    private ApprovalRequest buildApprovalRequest(WorkOrder order, String stage, ApprovalSubmissionRequest request) {
        String companyId = order.getId().getCompanyId();
        String orderId = order.getId().getOrderId();
        String idempotencyKey = buildIdempotencyKey(companyId, orderId, stage);

        String callbackUrl = resolveCallbackUrl();
        String content = ACT_STAGE.equals(stage)
            ? buildActualApprovalContent(order)
            : buildPlanApprovalContent(order);

        List<ApprovalStepRequest> steps = request.steps().stream()
            .map(this::toApprovalStepRequest)
            .collect(Collectors.toList());

        return new ApprovalRequest(
            buildTitle(stage, order.getName()),
            REF_ENTITY,
            orderId,
            stage,
            content,
            order.getFileGroupId(),
            callbackUrl,
            idempotencyKey,
            steps
        );
    }

    private String buildIdempotencyKey(String companyId, String orderId, String stage) {
        String randomHex = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return String.format("%s_%s_%s_%s_%s", companyId, REF_ENTITY, orderId, stage, randomHex);
    }

    private String resolveCallbackUrl() {
        String base = webhookProperties.getCallbackBase();
        if (base.endsWith("/") && CALLBACK_PATH.startsWith("/")) {
            return base.substring(0, base.length() - 1) + CALLBACK_PATH;
        } else if (!base.endsWith("/") && !CALLBACK_PATH.startsWith("/")) {
            return base + "/" + CALLBACK_PATH;
        }
        return base + CALLBACK_PATH;
    }

    private String buildTitle(String stage, String name) {
        if (ACT_STAGE.equals(stage)) {
            return "작업지시 실적 결재: " + name;
        }
        return "작업지시 계획 결재: " + name;
    }

    private String buildPlanApprovalContent(WorkOrder order) {
        return "<h3>작업지시 계획 결재 요청</h3><p>작업지시 번호: " + order.getId().getOrderId() + "</p>";
    }

    private String buildActualApprovalContent(WorkOrder order) {
        return "<h3>작업지시 실적 결재 요청</h3><p>작업지시 번호: " + order.getId().getOrderId() + "</p>";
    }

    private String normalizeStage(String requestedStage, String currentStage) {
        if (requestedStage != null && !requestedStage.isBlank()) {
            String upper = requestedStage.toUpperCase();
            if (!PLAN_STAGE.equals(upper) && !ACT_STAGE.equals(upper)) {
                throw new IllegalArgumentException("지원하지 않는 결재 단계입니다: " + requestedStage);
            }
            return upper;
        }
        if (PLAN_STAGE.equals(currentStage) || ACT_STAGE.equals(currentStage)) {
            return currentStage;
        }
        return PLAN_STAGE;
    }

    private ApprovalStepRequest toApprovalStepRequest(ApprovalLineStepRequest step) {
        return new ApprovalStepRequest(step.stepNo(), step.memberId(), step.decision());
    }

    @Transactional
    public void applyApprovalStatus(String workOrderId, String stage, ApprovalStatusTransition transition) {
        WorkOrder order = repository
            .findByIdCompanyIdAndIdOrderId(MemberUserDetailsService.DEFAULT_COMPANY, workOrderId)
            .orElseThrow(() -> new com.cmms11.common.error.NotFoundException("WorkOrder not found: " + workOrderId));

        order.setStage(stage);

        switch (transition) {
            case APPROVED -> order.setStatus("APPRV");
            case REJECTED -> order.setStatus("REJCT");
            case CANCELLED -> {
                order.setStatus("DRAFT");
                order.setApprovalId(null);
            }
            default -> {
            }
        }

        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(SYSTEM_ACTOR);
        repository.save(order);
    }
}
