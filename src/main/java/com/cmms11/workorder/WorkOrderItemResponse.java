package com.cmms11.workorder;

public record WorkOrderItemResponse(
    Integer lineNo,
    String name,
    String method,
    String result,
    String note
) {
    public static WorkOrderItemResponse from(WorkOrderItem entity) {
        Integer lineNo = entity.getId() != null ? entity.getId().getLineNo() : null;
        return new WorkOrderItemResponse(
            lineNo,
            entity.getName(),
            entity.getMethod(),
            entity.getResult(),
            entity.getNote()
        );
    }
}

