package com.cmms11.plant;

/**
 * 설비 CSV 업로드용 DTO
 * 기본정보 + 제조사 정보만 포함
 */
public record PlantUploadDto(
    String plantId,      // "(자동생성)" 또는 실제 ID
    String name,         // 필수
    String assetId,
    String siteId,
    String deptId,
    String funcId,
    String makerName,
    String model,
    String serial,
    String spec,
    String note
) {}

