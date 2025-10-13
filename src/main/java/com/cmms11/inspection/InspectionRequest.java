package com.cmms11.inspection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

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
    @Size(max = 10) String stage,
    @Size(max = 10) String refEntity,
    @Size(max = 10) String refId,
    @Size(max = 10) String refStage,
    @Size(max = 10) String approvalId,
    @Size(max = 10) String fileGroupId,
    @Size(max = 500) String note,
    @Valid List<InspectionItemRequest> items
) {
    public InspectionRequest {
        // Ensure a mutable list so Spring's DataBinder can grow items[index]
        items = (items == null) ? new ArrayList<>() : new ArrayList<>(items);
    }
}
