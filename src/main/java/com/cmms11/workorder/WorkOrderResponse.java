package com.cmms11.workorder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
    String stage,
    String refEntity,
    String refId,
    String refStage,
    String approvalId,
    String fileGroupId,
    String note,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
    String updatedBy,
    List<WorkOrderItemResponse> items
) {
    public WorkOrderResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static WorkOrderResponse from(WorkOrder workOrder) {
        return from(workOrder, List.of());
    }

    public static WorkOrderResponse from(WorkOrder workOrder, List<WorkOrderItem> itemEntities) {
        String orderId = workOrder.getId() != null ? workOrder.getId().getOrderId() : null;
        List<WorkOrderItemResponse> itemResponses = itemEntities
            .stream()
            .map(WorkOrderItemResponse::from)
            .collect(Collectors.toList());
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
            workOrder.getStage(),
            workOrder.getRefEntity(),
            workOrder.getRefId(),
            workOrder.getRefStage(),
            workOrder.getApprovalId(),
            workOrder.getFileGroupId(),
            workOrder.getNote(),
            workOrder.getCreatedAt(),
            workOrder.getCreatedBy(),
            workOrder.getUpdatedAt(),
            workOrder.getUpdatedBy(),
            itemResponses
        );
    }
}
