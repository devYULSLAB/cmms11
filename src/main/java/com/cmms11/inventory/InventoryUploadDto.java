package com.cmms11.inventory;

/**
 * 자재/재고 CSV 업로드용 DTO
 * 기본정보 + 제조사 정보만 포함
 */
public record InventoryUploadDto(
    String inventoryId,  // "(자동생성)" 또는 실제 ID
    String name,         // 필수
    String unit,
    String makerName,
    String model,
    String serial,
    String spec,
    String note
) {}

