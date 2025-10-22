package com.cmms11.approval;

import jakarta.validation.constraints.Size;

/**
 * 결재 처리 요청 DTO.
 */
public record ApprovalDecisionRequest(
    @Size(max = 500) String comment
) {
}
