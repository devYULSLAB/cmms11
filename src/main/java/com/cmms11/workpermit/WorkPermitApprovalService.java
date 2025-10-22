package com.cmms11.workpermit;

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
public class WorkPermitApprovalService {

    private static final String REF_ENTITY = "WPER";
    private static final String PLAN_STAGE = "PLN";
    private static final String ACT_STAGE = "ACT";
    private static final String CALLBACK_PATH = "/api/work-permits/approvals/webhook";
    private static final String STATUS_SUBMITTED = "SUBMT";
    private static final String SYSTEM_ACTOR = "system";

    private final WorkPermitRepository repository;
    private final ApprovalClient approvalClient;
    private final ApprovalWebhookProperties webhookProperties;

    public WorkPermitApprovalService(
        WorkPermitRepository repository,
        ApprovalClient approvalClient,
        ApprovalWebhookProperties webhookProperties
    ) {
        this.repository = repository;
        this.approvalClient = approvalClient;
        this.webhookProperties = webhookProperties;
    }

    @Transactional
    public ApprovalResponse submitApproval(String permitId, ApprovalSubmissionRequest request) {
        WorkPermit permit = repository
            .findByIdCompanyIdAndIdPermitId(MemberUserDetailsService.DEFAULT_COMPANY, permitId)
            .orElseThrow(() -> new com.cmms11.common.error.NotFoundException("WorkPermit not found: " + permitId));

        String stage = normalizeStage(request.stage(), permit.getStage());
        if (!"DRAFT".equals(permit.getStatus())) {
            throw new IllegalStateException("작성 중(DRAFT) 문서만 결재 상신할 수 있습니다.");
        }

        ApprovalRequest approvalRequest = buildApprovalRequest(permit, stage, request);
        ApprovalResponse approval = approvalClient.submitApproval(approvalRequest);

        permit.setApprovalId(approval.approvalId());
        permit.setStage(stage);
        permit.setStatus(STATUS_SUBMITTED);
        permit.setUpdatedAt(LocalDateTime.now());
        permit.setUpdatedBy(MemberUserDetailsService.getCurrentMemberId());
        repository.save(permit);

        return approval;
    }

    private ApprovalRequest buildApprovalRequest(WorkPermit permit, String stage, ApprovalSubmissionRequest request) {
        String companyId = permit.getId().getCompanyId();
        String idempotencyKey = buildIdempotencyKey(companyId, permit.getId().getPermitId(), stage);

        String callbackUrl = resolveCallbackUrl();
        String content = buildApprovalContent(permit);

        List<ApprovalStepRequest> steps = request.steps().stream()
            .map(this::toApprovalStepRequest)
            .collect(Collectors.toList());

        return new ApprovalRequest(
            buildTitle(stage, permit.getName()),
            REF_ENTITY,
            permit.getId().getPermitId(),
            stage,
            content,
            permit.getFileGroupId(),
            callbackUrl,
            idempotencyKey,
            steps
        );
    }

    private ApprovalStepRequest toApprovalStepRequest(ApprovalLineStepRequest step) {
        return new ApprovalStepRequest(step.stepNo(), step.memberId(), step.decision());
    }

    private String buildIdempotencyKey(String companyId, String permitId, String stage) {
        String randomHex = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return String.format("%s_%s_%s_%s_%s", companyId, REF_ENTITY, permitId, stage, randomHex);
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

    private String buildApprovalContent(WorkPermit permit) {
        return "<h3>작업허가 결재 요청</h3><p>허가 번호: " + permit.getId().getPermitId() + "</p>";
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

    private String buildTitle(String stage, String name) {
        if (ACT_STAGE.equals(stage)) {
            return "작업허가 실적 결재: " + name;
        }
        return "작업허가 결재: " + name;
    }

    @Transactional
    public void applyApprovalStatus(String permitId, String stage, ApprovalStatusTransition transition) {
        WorkPermit permit = repository
            .findByIdCompanyIdAndIdPermitId(MemberUserDetailsService.DEFAULT_COMPANY, permitId)
            .orElseThrow(() -> new com.cmms11.common.error.NotFoundException("WorkPermit not found: " + permitId));

        permit.setStage(stage);

        switch (transition) {
            case APPROVED -> permit.setStatus("APPRV");
            case REJECTED -> permit.setStatus("REJCT");
            case CANCELLED -> {
                permit.setStatus("DRAFT");
                permit.setApprovalId(null);
            }
            default -> {
            }
        }

        permit.setUpdatedAt(LocalDateTime.now());
        permit.setUpdatedBy(SYSTEM_ACTOR);
        repository.save(permit);
    }
}
