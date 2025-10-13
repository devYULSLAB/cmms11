package com.cmms11.approval;

import java.time.LocalDateTime;

/**
 * 이름: ApprovalInboxResponse
 * 작성자: codex
 * 작성일: 2025-10-15
 * 프로그램 개요: approval_inbox 조회 응답 DTO.
 */
public record ApprovalInboxResponse(
    String inboxId,
    String approvalId,
    Integer stepNo,
    String memberId,
    String inboxType,
    String isRead,
    LocalDateTime readAt,
    String title,
    String refEntity,
    String refId,
    String submittedBy,
    LocalDateTime submittedAt,
    String decision,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ApprovalInboxResponse from(ApprovalInbox inbox) {
        String inboxId = inbox.getId() != null ? inbox.getId().getInboxId() : null;
        return new ApprovalInboxResponse(
            inboxId,
            inbox.getApprovalId(),
            inbox.getStepNo(),
            inbox.getMemberId(),
            inbox.getInboxType(),
            inbox.getIsRead(),
            inbox.getReadAt(),
            inbox.getTitle(),
            inbox.getRefEntity(),
            inbox.getRefId(),
            inbox.getSubmittedBy(),
            inbox.getSubmittedAt(),
            inbox.getDecision(),
            inbox.getCreatedAt(),
            inbox.getUpdatedAt()
        );
    }
}
