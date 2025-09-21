package com.cmms11.workpermit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 이름: WorkPermitRequest
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 작업허가 생성/수정 요청 DTO.
 */
public record WorkPermitRequest(
    @Size(max = 10) String workPermitId,
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 10) String plantId,
    @NotBlank @Size(max = 5) String jobId,
    @NotBlank @Size(max = 5) String siteId,
    @NotBlank @Size(max = 5) String deptId,
    @Size(max = 5) String memberId,
    LocalDate plannedDate,
    LocalDate actualDate,
    @Size(max = 500) String workSummary,
    @Size(max = 500) String hazardFactor,
    @Size(max = 500) String safetyFactor,
    String checksheetJson,
    @Size(max = 10) String status,
    @Size(max = 10) String fileGroupId,
    @Size(max = 500) String note
) {
}
