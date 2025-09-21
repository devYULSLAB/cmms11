package com.cmms11.workorder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 이름: WorkOrderRequest
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 작업지시 생성/수정 요청 DTO.
 */
public record WorkOrderRequest(
    @Size(max = 10) String workOrderId,
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 10) String plantId,
    @NotBlank @Size(max = 5) String jobId,
    @NotBlank @Size(max = 5) String siteId,
    @NotBlank @Size(max = 5) String deptId,
    @Size(max = 5) String memberId,
    LocalDate plannedDate,
    BigDecimal plannedCost,
    BigDecimal plannedLabor,
    LocalDate actualDate,
    BigDecimal actualCost,
    BigDecimal actualLabor,
    @Size(max = 10) String status,
    @Size(max = 10) String fileGroupId,
    @Size(max = 500) String note
) {
}
