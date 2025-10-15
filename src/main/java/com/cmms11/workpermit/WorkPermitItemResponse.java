package com.cmms11.workpermit;

import java.time.LocalDateTime;

/**
 * 이름: WorkPermitItemResponse
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 작업허가 항목 응답 DTO.
 */
public record WorkPermitItemResponse(
    String name,
    String signature,
    String note,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
    String updatedBy
) {
    public static WorkPermitItemResponse from(WorkPermitItem item) {
        return new WorkPermitItemResponse(
            item.getName(),
            item.getSignature(),
            item.getNote(),
            item.getCreatedAt(),
            item.getCreatedBy(),
            item.getUpdatedAt(),
            item.getUpdatedBy()
        );
    }
}

