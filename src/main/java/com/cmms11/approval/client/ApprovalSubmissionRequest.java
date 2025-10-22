package com.cmms11.approval.client;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 결재 상신 요청 페이로드.
 */
public record ApprovalSubmissionRequest(
    String stage,
    @NotEmpty List<@Valid ApprovalLineStepRequest> steps
) {
}
