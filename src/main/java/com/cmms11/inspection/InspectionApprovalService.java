package com.cmms11.inspection;

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

/**
 * Inspection 결재 상신 로직을 담당.
 */
@Service
public class InspectionApprovalService {

    private static final String REF_ENTITY = "INSP";
    private static final String PLAN_STAGE = "PLN";
    private static final String ACT_STAGE = "ACT";
    private static final String CALLBACK_PATH = "/api/inspections/approvals/webhook";
    private static final String APPROVAL_STATUS_SUBMITTED = "SUBMT";
    private static final String SYSTEM_ACTOR = "system";

    private final InspectionRepository inspectionRepository;
    private final ApprovalClient approvalClient;
    private final ApprovalWebhookProperties webhookProperties;

    public InspectionApprovalService(
        InspectionRepository inspectionRepository,
        ApprovalClient approvalClient,
        ApprovalWebhookProperties webhookProperties
    ) {
        this.inspectionRepository = inspectionRepository;
        this.approvalClient = approvalClient;
        this.webhookProperties = webhookProperties;
    }

    @Transactional
    public ApprovalResponse submitApproval(String inspectionId, ApprovalSubmissionRequest request) {
        Inspection inspection = inspectionRepository
            .findByIdCompanyIdAndIdInspectionId(MemberUserDetailsService.DEFAULT_COMPANY, inspectionId)
            .orElseThrow(() -> new com.cmms11.common.error.NotFoundException("Inspection not found: " + inspectionId));

        String stage = normalizeStage(request.stage(), inspection.getStage());
        validateStatusForSubmission(inspection);

        ApprovalRequest approvalRequest = buildApprovalRequest(inspection, stage, request);
        ApprovalResponse approval = approvalClient.submitApproval(approvalRequest);

        inspection.setApprovalId(approval.approvalId());
        inspection.setStatus(APPROVAL_STATUS_SUBMITTED);
        inspection.setUpdatedAt(LocalDateTime.now());
        inspection.setUpdatedBy(MemberUserDetailsService.getCurrentMemberId());
        inspectionRepository.save(inspection);

        return approval;
    }

    private void validateStatusForSubmission(Inspection inspection) {
        if (!"DRAFT".equals(inspection.getStatus())) {
            throw new IllegalStateException("작성 중(DRAFT) 문서만 결재 상신할 수 있습니다.");
        }
    }

    private ApprovalRequest buildApprovalRequest(
        Inspection inspection,
        String stage,
        ApprovalSubmissionRequest request
    ) {
        String companyId = inspection.getId().getCompanyId();
        String idempotencyKey = buildIdempotencyKey(companyId, inspection.getId().getInspectionId(), stage);

        String callbackUrl = resolveCallbackUrl();
        String content = PLAN_STAGE.equals(stage)
            ? buildPlanApprovalContent(inspection)
            : buildActualApprovalContent(inspection);

        List<ApprovalStepRequest> steps = request.steps().stream()
            .map(this::toApprovalStepRequest)
            .collect(Collectors.toList());

        return new ApprovalRequest(
            buildTitle(stage, inspection.getName()),
            REF_ENTITY,
            inspection.getId().getInspectionId(),
            stage,
            content,
            inspection.getFileGroupId(),
            callbackUrl,
            idempotencyKey,
            steps
        );
    }

    private String buildIdempotencyKey(String companyId, String inspectionId, String stage) {
        String randomHex = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return String.format("%s_%s_%s_%s_%s", companyId, REF_ENTITY, inspectionId, stage, randomHex);
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

    private String buildTitle(String stage, String inspectionName) {
        if (ACT_STAGE.equals(stage)) {
            return "점검 실적 결재: " + inspectionName;
        }
        return "점검 계획 결재: " + inspectionName;
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

    private String buildPlanApprovalContent(Inspection inspection) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>점검 계획 결재 요청</h3>");
        sb.append("<table style='border-collapse:collapse; width:100%;'>");
        sb.append("<tr><th style='border:1px solid #ddd; padding:8px; background:#f5f5f5;'>항목</th>");
        sb.append("<th style='border:1px solid #ddd; padding:8px; background:#f5f5f5;'>내용</th></tr>");

        appendRow(sb, "점검 ID", inspection.getId().getInspectionId());
        appendRow(sb, "점검명", inspection.getName());
        appendRow(sb, "설비", inspection.getPlantId());
        appendRow(sb, "담당자", inspection.getMemberId());
        appendRow(sb, "계획일", inspection.getPlannedDate() != null ? inspection.getPlannedDate().toString() : "-");

        sb.append("</table>");

        if (inspection.getNote() != null && !inspection.getNote().isEmpty()) {
            sb.append("<p style='margin-top:15px;'><strong>비고:</strong></p>");
            sb.append("<p>").append(inspection.getNote()).append("</p>");
        }

        return sb.toString();
    }

    private String buildActualApprovalContent(Inspection inspection) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>점검 실적 결재 요청</h3>");
        sb.append("<table style='border-collapse:collapse; width:100%;'>");
        sb.append("<tr><th style='border:1px solid #ddd; padding:8px; background:#f5f5f5;'>항목</th>");
        sb.append("<th style='border:1px solid #ddd; padding:8px; background:#f5f5f5;'>내용</th></tr>");

        appendRow(sb, "점검 ID", inspection.getId().getInspectionId());
        appendRow(sb, "점검명", inspection.getName());
        appendRow(sb, "설비", inspection.getPlantId());
        appendRow(sb, "담당자", inspection.getMemberId());
        appendRow(sb, "계획일", inspection.getPlannedDate() != null ? inspection.getPlannedDate().toString() : "-");
        appendRow(sb, "실적일", inspection.getActualDate() != null ? inspection.getActualDate().toString() : "-");

        sb.append("</table>");

        if (inspection.getNote() != null && !inspection.getNote().isEmpty()) {
            sb.append("<p style='margin-top:15px;'><strong>비고:</strong></p>");
            sb.append("<p>").append(inspection.getNote()).append("</p>");
        }

        return sb.toString();
    }

    private void appendRow(StringBuilder sb, String label, String value) {
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>").append(label).append("</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(value != null ? value : "-").append("</td></tr>");
    }

    @Transactional
    public void applyApprovalStatus(
        String inspectionId,
        String stage,
        ApprovalStatusTransition transition
    ) {
        Inspection inspection = inspectionRepository
            .findByIdCompanyIdAndIdInspectionId(MemberUserDetailsService.DEFAULT_COMPANY, inspectionId)
            .orElseThrow(() -> new com.cmms11.common.error.NotFoundException("Inspection not found: " + inspectionId));

        inspection.setStage(stage);

        switch (transition) {
            case APPROVED -> inspection.setStatus("APPRV");
            case REJECTED -> inspection.setStatus("REJCT");
            case CANCELLED -> {
                inspection.setStatus("DRAFT");
                inspection.setApprovalId(null);
            }
            default -> {
            }
        }
        inspection.setUpdatedAt(LocalDateTime.now());
        inspection.setUpdatedBy(SYSTEM_ACTOR);
        inspectionRepository.save(inspection);
    }


    private ApprovalStepRequest toApprovalStepRequest(ApprovalLineStepRequest step) {
        return new ApprovalStepRequest(
            step.stepNo(),
            step.memberId(),
            step.decision()
        );
    }
}
