package com.cmms11.approval;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 이름: ApprovalStepRequest
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 결재 단계 요청 DTO.
 */
public record ApprovalStepRequest(
    Integer stepNo,
    @NotBlank @Size(max = 5) String memberId,
    @NotBlank @Size(max = 10) String decision
) {
}
