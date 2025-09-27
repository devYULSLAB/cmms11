package com.cmms11.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 이름: InventoryRequest
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 재고 마스터 생성/수정 요청을 표현하는 DTO.
 */
public record InventoryRequest(
    @Size(max = 10) String inventoryId,
    @Size(max = 100) String name,
    @Size(max = 5) String assetId,
    @Size(max = 5) String deptId,
    @Size(max = 100) String makerName,
    @Size(max = 100) String spec,
    @Size(max = 100) String model,
    @Size(max = 100) String serial,
    @Size(max = 10) String fileGroupId,
    @Size(max = 500) String note,
    @Size(max = 10) String status
) {
}
