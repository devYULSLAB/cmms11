package com.cmms11.workorder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 이름: WorkOrderResponse
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 작업지시 응답 DTO.
 */
public record WorkOrderResponse(
    String orderId,
    String name,
    String plantId,
    String jobId,
    String siteId,
    String deptId,
    String memberId,
    LocalDate plannedDate,
    BigDecimal plannedCost,
    BigDecimal plannedLabor,
    LocalDate actualDate,
    BigDecimal actualCost,
    BigDecimal actualLabor,
    String status,
    String fileGroupId,
    String note,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
    String updatedBy
) {
    public static WorkOrderResponse from(WorkOrder workOrder) {
        String orderId = workOrder.getId() != null ? workOrder.getId().getOrderId() : null;
        return new WorkOrderResponse(
            orderId,
            workOrder.getName(),
            workOrder.getPlantId(),
            workOrder.getJobId(),
            workOrder.getSiteId(),
            workOrder.getDeptId(),
            workOrder.getMemberId(),
            workOrder.getPlannedDate(),
            workOrder.getPlannedCost(),
            workOrder.getPlannedLabor(),
            workOrder.getActualDate(),
            workOrder.getActualCost(),
            workOrder.getActualLabor(),
            workOrder.getStatus(),
            workOrder.getFileGroupId(),
            workOrder.getNote(),
            workOrder.getCreatedAt(),
            workOrder.getCreatedBy(),
            workOrder.getUpdatedAt(),
            workOrder.getUpdatedBy()
        );
    }
}
