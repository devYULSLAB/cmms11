package com.cmms11.approval;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

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
    @Size(max = 10) String decision,
    @Size(max = 10) String result,
    LocalDateTime decidedAt,
    @Size(max = 500) String comment
) {
}
