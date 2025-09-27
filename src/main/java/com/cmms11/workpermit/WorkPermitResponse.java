package com.cmms11.workpermit;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 이름: WorkPermitResponse
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 작업허가 응답 DTO.
 */
public record WorkPermitResponse(
    String permitId,
    String name,
    String plantId,
    String jobId,
    String siteId,
    String deptId,
    String memberId,
    LocalDate plannedDate,
    LocalDate actualDate,
    String workSummary,
    String hazardFactor,
    String safetyFactor,
    String checksheetJson,
    String status,
    String fileGroupId,
    String note,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
    String updatedBy
) {
    public static WorkPermitResponse from(WorkPermit workPermit) {
        String permitId = workPermit.getId() != null ? workPermit.getId().getPermitId() : null;
        return new WorkPermitResponse(
            permitId,
            workPermit.getName(),
            workPermit.getPlantId(),
            workPermit.getJobId(),
            workPermit.getSiteId(),
            workPermit.getDeptId(),
            workPermit.getMemberId(),
            workPermit.getPlannedDate(),
            workPermit.getActualDate(),
            workPermit.getWorkSummary(),
            workPermit.getHazardFactor(),
            workPermit.getSafetyFactor(),
            workPermit.getChecksheetJson(),
            workPermit.getStatus(),
            workPermit.getFileGroupId(),
            workPermit.getNote(),
            workPermit.getCreatedAt(),
            workPermit.getCreatedBy(),
            workPermit.getUpdatedAt(),
            workPermit.getUpdatedBy()
        );
    }
}
