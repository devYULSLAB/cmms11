package com.cmms11.plant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 이름: PlantRequest
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 설비 생성/수정 요청 DTO.
 */
public record PlantRequest(
    @Size(max = 10) String plantId,
    @Size(max = 100) String name,
    @Size(max = 5) String assetId,
    @Size(max = 5) String siteId,
    @Size(max = 5) String deptId,
    @Size(max = 5) String funcId,
    @Size(max = 100) String makerName,
    @Size(max = 100) String spec,
    @Size(max = 100) String model,
    @Size(max = 100) String serial,
    LocalDate installDate,
    @Size(max = 5) String depreId,
    Integer deprePeriod,
    BigDecimal purchaseCost,
    BigDecimal residualValue,
    @Size(max = 1) String inspectionYn,
    @Size(max = 1) String psmYn,
    @Size(max = 1) String workpermitYn,
    Integer inspectionInterval,
    LocalDate lastInspection,
    LocalDate nextInspection,
    @Size(max = 10) String fileGroupId,
    @Size(max = 500) String note
) {
}
