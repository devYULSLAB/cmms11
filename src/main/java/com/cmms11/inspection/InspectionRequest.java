package com.cmms11.inspection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 이름: InspectionRequest
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 예방점검 생성/수정 요청 DTO.
 */
public record InspectionRequest(
    @Size(max = 10) String inspectionId,
    @Size(max = 100) String name,
    @Size(max = 10) String plantId,
    @Size(max = 5) String jobId,
    @Size(max = 5) String siteId,
    @Size(max = 5) String deptId,
    @Size(max = 5) String memberId,
    LocalDate plannedDate,
    LocalDate actualDate,
    @Size(max = 10) String status,
    @Size(max = 10) String fileGroupId,
    @Size(max = 500) String note,
    @Valid List<InspectionItemRequest> items
) {
    public InspectionRequest {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
