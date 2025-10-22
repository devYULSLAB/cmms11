package com.cmms11.approval.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 결재선 입력 항목.
 */
public record ApprovalLineStepRequest(
    Integer stepNo,
    @NotBlank @Size(max = 5) String memberId,
    @NotBlank @Size(max = 10) String decision
) {
}
