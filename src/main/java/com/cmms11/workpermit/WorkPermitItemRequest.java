package com.cmms11.workpermit;

import jakarta.validation.constraints.Size;

public record WorkPermitItemRequest(
    @Size(max = 100) String name,
    String signature,
    @Size(max = 500) String note
) {
    public WorkPermitItemRequest() {
        this(null, null, null);
    }
}

