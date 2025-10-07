package com.cmms11.plant;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 이름: PlantResponse
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 설비 조회 응답 DTO.
 */
public record PlantResponse(
    String plantId,
    String name,
    String assetId,
    String siteId,
    String deptId,
    String funcId,
    String makerName,
    String spec,
    String model,
    String serial,
    LocalDate installDate,
    String depreId,
    Integer deprePeriod,
    BigDecimal purchaseCost,
    BigDecimal residualValue,
    String inspectionYn,
    String psmYn,
    String workpermitYn,
    Integer inspectionInterval,
    LocalDate lastInspection,
    LocalDate nextInspection,
    String fileGroupId,
    String note,
    String deleteMark,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
    String updatedBy
) {

    public static PlantResponse from(Plant plant) {
        return new PlantResponse(
            plant.getId() != null ? plant.getId().getPlantId() : null,
            plant.getName(),
            plant.getAssetId(),
            plant.getSiteId(),
            plant.getDeptId(),
            plant.getFuncId(),
            plant.getMakerName(),
            plant.getSpec(),
            plant.getModel(),
            plant.getSerial(),
            plant.getInstallDate(),
            plant.getDepreId(),
            plant.getDeprePeriod(),
            plant.getPurchaseCost(),
            plant.getResidualValue(),
            plant.getInspectionYn(),
            plant.getPsmYn(),
            plant.getWorkpermitYn(),
            plant.getInspectionInterval(),
            plant.getLastInspection(),
            plant.getNextInspection(),
            plant.getFileGroupId(),
            plant.getNote(),
            plant.getDeleteMark(),
            plant.getCreatedAt(),
            plant.getCreatedBy(),
            plant.getUpdatedAt(),
            plant.getUpdatedBy()
        );
    }
}
