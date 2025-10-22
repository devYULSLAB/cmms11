package com.cmms11.approval;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 이름: ApprovalRequest
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 결재 생성/수정 요청 DTO.
 */
public record ApprovalRequest(
    @NotBlank @Size(max = 100) String title,
    @NotBlank @Size(max = 64) String refEntity,
    @NotBlank @Size(max = 10) String refId,
    @NotBlank @Size(max = 10) String refStage,
    String content,
    @Size(max = 10) String fileGroupId,
    @NotBlank @Size(max = 255) String callbackUrl,
    @NotBlank @Size(max = 100) String idempotencyKey,
    @Valid List<ApprovalStepRequest> steps
) {
}
