package com.cmms11.inspection;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InspectionItemRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 100) String method,
    @Size(max = 50) String minVal,
    @Size(max = 50) String maxVal,
    @Size(max = 50) String stdVal,
    @Size(max = 50) String unit,
    @Size(max = 50) String resultVal,
    @Size(max = 500) String note
) {
    public InspectionItemRequest() {
        this(null, null, null, null, null, null, null, null);
    }
}
