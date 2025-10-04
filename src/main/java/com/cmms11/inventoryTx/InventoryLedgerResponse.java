package com.cmms11.inventoryTx;

import java.math.BigDecimal;

/**
 * 이름: InventoryLedgerResponse
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 재고원장 응답 DTO.
 */
public record InventoryLedgerResponse(
    String storageId,
    String inventoryId,
    String txDate,
    String txType,
    String refNo,
    BigDecimal beginQty,
    BigDecimal beginAmount,
    BigDecimal inQty,
    BigDecimal inAmount,
    BigDecimal outQty,
    BigDecimal outAmount,
    BigDecimal moveQty,
    BigDecimal moveAmount,
    BigDecimal adjQty,
    BigDecimal adjAmount,
    BigDecimal endQty,
    BigDecimal endAmount
) {}
