package com.cmms11.approval;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Webhook 전송용 이벤트 페이로드.
 */
public record ApprovalEventPayload(
    String companyId,
    String approvalId,
    String refEntity,
    String refId,
    String refStage,
    String status,
    ApprovalEventType eventType,
    LocalDateTime occurredAt,
    String actorId,
    String comment,
    String callbackUrl,
    String idempotencyKey,
    List<ApprovalEventStep> steps
) {
    public record ApprovalEventStep(
        Integer stepNo,
        String memberId,
        String decision,
        String result,
        LocalDateTime decidedAt,
        String comment
    ) {
    }
}
