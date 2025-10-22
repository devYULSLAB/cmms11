package com.cmms11.approval;

/**
 * Outbox 이벤트 처리 상태.
 */
public enum ApprovalOutboxStatus {
    PENDING,
    SENT,
    FAILED
}
