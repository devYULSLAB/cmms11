package com.cmms11.inspection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 이름: InspectionResponse
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 예방점검 응답 DTO.
 */
public record InspectionResponse(
    String inspectionId,
    String name,
    String plantId,
    String jobId,
    String siteId,
    String deptId,
    String memberId,
    LocalDate plannedDate,
    LocalDate actualDate,
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
    List<InspectionItemResponse> items
) {
    public InspectionResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static InspectionResponse from(Inspection inspection) {
        return from(inspection, List.of());
    }

    public static InspectionResponse from(Inspection inspection, List<InspectionItem> itemEntities) {
        String inspectionId = inspection.getId() != null ? inspection.getId().getInspectionId() : null;
        List<InspectionItemResponse> itemResponses = itemEntities
            .stream()
            .map(InspectionItemResponse::from)
            .collect(Collectors.toList());
        return new InspectionResponse(
            inspectionId,
            inspection.getName(),
            inspection.getPlantId(),
            inspection.getJobId(),
            inspection.getSiteId(),
            inspection.getDeptId(),
            inspection.getMemberId(),
            inspection.getPlannedDate(),
            inspection.getActualDate(),
            inspection.getStatus(),
            inspection.getStage(),
            inspection.getRefEntity(),
            inspection.getRefId(),
            inspection.getRefStage(),
            inspection.getApprovalId(),
            inspection.getFileGroupId(),
            inspection.getNote(),
            inspection.getCreatedAt(),
            inspection.getCreatedBy(),
            inspection.getUpdatedAt(),
            inspection.getUpdatedBy(),
            itemResponses
        );
    }

    /**
     * ID를 null로 설정한 새로운 인스턴스를 반환
     * 계획 데이터를 복사하여 새로운 실적을 생성할 때 사용
     */
    public InspectionResponse withNullId() {
        return new InspectionResponse(
            null,  // inspectionId를 null로 설정
            this.name,
            this.plantId,
            this.jobId,
            this.siteId,
            this.deptId,
            this.memberId,
            this.plannedDate,
            this.actualDate,
            this.status,
            this.stage,
            this.refEntity,
            this.refId,
            this.refStage,
            this.approvalId,
            this.fileGroupId,
            this.note,
            this.createdAt,
            this.createdBy,
            this.updatedAt,
            this.updatedBy,
            this.items
        );
    }
}
