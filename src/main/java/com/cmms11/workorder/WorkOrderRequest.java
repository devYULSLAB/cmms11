package com.cmms11.workorder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 이름: WorkOrderRequest
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 작업지시 생성/수정 요청 DTO.
 */
public record WorkOrderRequest(
    @Size(max = 10) String orderId,
    @Size(max = 100) String name,
    @Size(max = 10) String plantId,
    @Size(max = 5) String jobId,
    @Size(max = 5) String siteId,
    @Size(max = 5) String deptId,
    @Size(max = 5) String memberId,
    LocalDate plannedDate,
    BigDecimal plannedCost,
    BigDecimal plannedLabor,
    LocalDate actualDate,
    BigDecimal actualCost,
    BigDecimal actualLabor,
    @Size(max = 10) String status,
    @Size(max = 10) String stage,
    @Size(max = 10) String refEntity,
    @Size(max = 10) String refId,
    @Size(max = 10) String refStage,
    @Size(max = 20) String approvalId,
    @Size(max = 10) String fileGroupId,
    @Size(max = 500) String note,
    @Valid List<WorkOrderItemRequest> items
) {
    public WorkOrderRequest {
        items = (items == null) ? new ArrayList<>() : new ArrayList<>(items);
    }
}
