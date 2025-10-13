package com.cmms11.approval;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이름: ApprovalResponse
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 결재 응답 DTO.
 */
public record ApprovalResponse(
    String approvalId,
    String title,
    String status,
    String refEntity,
    String refId,
    String refStage,
    String content,
    String fileGroupId,
    LocalDateTime submittedAt,
    LocalDateTime completedAt,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
    String updatedBy,
    List<ApprovalStepResponse> steps
) {
    public static ApprovalResponse from(Approval approval, List<ApprovalStepResponse> steps) {
        String approvalId = approval.getId() != null ? approval.getId().getApprovalId() : null;
        return new ApprovalResponse(
            approvalId,
            approval.getTitle(),
            approval.getStatus(),
            approval.getRefEntity(),
            approval.getRefId(),
            approval.getRefStage(),
            approval.getContent(),
            approval.getFileGroupId(),
            approval.getSubmittedAt(),
            approval.getCompletedAt(),
            approval.getCreatedAt(),
            approval.getCreatedBy(),
            approval.getUpdatedAt(),
            approval.getUpdatedBy(),
            steps
        );
    }
}
