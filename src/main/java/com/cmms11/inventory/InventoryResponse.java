package com.cmms11.inventory;

import java.time.LocalDateTime;

/**
 * 이름: InventoryResponse
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 재고 마스터 응답 페이로드를 표현하는 DTO.
 */
public record InventoryResponse(
    String inventoryId,
    String name,
    String assetId,
    String deptId,
    String makerName,
    String spec,
    String model,
    String serial,
    String fileGroupId,
    String note,
    String deleteMark,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
    String updatedBy
) {
    public static InventoryResponse from(Inventory inventory) {
        String inventoryId = inventory.getId() != null ? inventory.getId().getInventoryId() : null;
        return new InventoryResponse(
            inventoryId,
            inventory.getName(),
            inventory.getAssetId(),
            inventory.getDeptId(),
            inventory.getMakerName(),
            inventory.getSpec(),
            inventory.getModel(),
            inventory.getSerial(),
            inventory.getFileGroupId(),
            inventory.getNote(),
            inventory.getDeleteMark(),
            inventory.getCreatedAt(),
            inventory.getCreatedBy(),
            inventory.getUpdatedAt(),
            inventory.getUpdatedBy()
        );
    }
}
