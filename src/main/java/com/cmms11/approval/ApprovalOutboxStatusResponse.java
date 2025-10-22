package com.cmms11.approval;

import java.time.LocalDateTime;

/**
 * Outbox 상태 요약 응답.
 */
public record ApprovalOutboxStatusResponse(
    long pendingCount,
    long failedCount,
    LocalDateTime oldestPendingCreatedAt
) {
}
