package com.cmms11.workorder;

import jakarta.validation.constraints.Size;

public record WorkOrderItemRequest(
    @Size(max = 100) String name,
    @Size(max = 100) String method,
    @Size(max = 100) String result,
    @Size(max = 500) String note
) {
    public WorkOrderItemRequest() { this(null, null, null, null); }
}

