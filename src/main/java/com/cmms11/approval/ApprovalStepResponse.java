package com.cmms11.approval;

import java.time.LocalDateTime;

/**
 * 이름: ApprovalStepResponse
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 결재 단계 응답 DTO.
 */
public record ApprovalStepResponse(
    Integer stepNo,
    String memberId,
    String decision,
    String result,
    LocalDateTime decidedAt,
    String comment
) {
    public static ApprovalStepResponse from(ApprovalStep step) {
        Integer stepNo = step.getId() != null ? step.getId().getStepNo() : null;
        return new ApprovalStepResponse(
            stepNo,
            step.getMemberId(),
            step.getDecision(),
            step.getResult(),
            step.getDecidedAt(),
            step.getComment()
        );
    }
}
