package com.cmms11.approval;

import java.time.LocalDateTime;

/**
 * Outbox 이벤트 상세 응답.
 */
public record ApprovalOutboxEventResponse(
    Long id,
    String approvalId,
    ApprovalEventType eventType,
    ApprovalOutboxStatus status,
    int retryCount,
    String lastErrorMessage,
    LocalDateTime lastAttemptAt,
    LocalDateTime nextAttemptAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ApprovalOutboxEventResponse from(ApprovalOutbox outbox) {
        return new ApprovalOutboxEventResponse(
            outbox.getId(),
            outbox.getApprovalId(),
            outbox.getEventType(),
            outbox.getStatus(),
            outbox.getRetryCount(),
            outbox.getLastErrorMessage(),
            outbox.getLastAttemptAt(),
            outbox.getNextAttemptAt(),
            outbox.getCreatedAt(),
            outbox.getUpdatedAt()
        );
    }
}
