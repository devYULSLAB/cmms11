package com.cmms11.workpermit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
    String stage,
    String refEntity,
    String refId,
    String refStage,
    String approvalId,
    String fileGroupId,
    String note,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
    String updatedBy,
    List<WorkPermitItemResponse> items
) {
    public WorkPermitResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static WorkPermitResponse from(WorkPermit workPermit) {
        return from(workPermit, List.of());
    }

    public static WorkPermitResponse from(WorkPermit workPermit, List<WorkPermitItem> itemEntities) {
        String permitId = workPermit.getId() != null ? workPermit.getId().getPermitId() : null;
        List<WorkPermitItemResponse> itemResponses = itemEntities
            .stream()
            .map(WorkPermitItemResponse::from)
            .collect(Collectors.toList());
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
            workPermit.getStage(),
            workPermit.getRefEntity(),
            workPermit.getRefId(),
            workPermit.getRefStage(),
            workPermit.getApprovalId(),
            workPermit.getFileGroupId(),
            workPermit.getNote(),
            workPermit.getCreatedAt(),
            workPermit.getCreatedBy(),
            workPermit.getUpdatedAt(),
            workPermit.getUpdatedBy(),
            itemResponses
        );
    }
}
