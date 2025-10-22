package com.cmms11.web.api;

import com.cmms11.approval.ApprovalResponse;
import com.cmms11.approval.client.ApprovalSubmissionRequest;
import com.cmms11.inspection.InspectionApprovalService;
import com.cmms11.inspection.InspectionItem;
import com.cmms11.inspection.InspectionRequest;
import com.cmms11.inspection.InspectionResponse;
import com.cmms11.inspection.InspectionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이름: InspectionApiController
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 점검 관리 API 컨트롤러 (JSON 반환)
 */
@RestController
@RequestMapping("/api/inspections")
public class InspectionApiController {

    private final InspectionService service;
    private final InspectionApprovalService approvalService;

    public InspectionApiController(InspectionService service, InspectionApprovalService approvalService) {
        this.service = service;
        this.approvalService = approvalService;
    }

    /**
     * 점검 목록 조회 (API)
     */
    @GetMapping
    public ResponseEntity<Page<InspectionResponse>> list(
        @RequestParam(required = false) String inspectionId,
        @RequestParam(required = false) String plantId,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String stage,
        @RequestParam(required = false) String plannedDateFrom,
        @RequestParam(required = false) String plannedDateTo,
        Pageable pageable
    ) {
        Page<InspectionResponse> page = service.list(
            inspectionId, plantId, name, status, stage, plannedDateFrom, plannedDateTo, pageable
        );
        return ResponseEntity.ok(page);
    }

    /**
     * 점검 단건 조회 (API)
     */
    @GetMapping("/{inspectionId}")
    public ResponseEntity<InspectionResponse> get(@PathVariable String inspectionId) {
        InspectionResponse response = service.get(inspectionId);
        return ResponseEntity.ok(response);
    }

    /**
     * 점검 항목 조회 (API)
     */
    @GetMapping("/{inspectionId}/items")
    public ResponseEntity<List<InspectionItem>> getItems(@PathVariable String inspectionId) {
        List<InspectionItem> items = service.getItems(inspectionId);
        return ResponseEntity.ok(items);
    }

    /**
     * 점검 생성 (API)
     */
    @PostMapping
    public ResponseEntity<InspectionResponse> create(@Valid @RequestBody InspectionRequest request) {
        InspectionResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 점검 수정 (API)
     */
    @PutMapping("/{inspectionId}")
    public ResponseEntity<InspectionResponse> update(
        @PathVariable String inspectionId,
        @Valid @RequestBody InspectionRequest request
    ) {
        InspectionResponse response = service.update(inspectionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 점검 삭제 (API)
     */
    @DeleteMapping("/{inspectionId}")
    public ResponseEntity<Void> delete(@PathVariable String inspectionId) {
        service.delete(inspectionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 계획 결재 상신 (API)
     */
    @PostMapping("/{inspectionId}/approvals")
    public ResponseEntity<ApprovalResponse> submitApproval(
        @PathVariable String inspectionId,
        @Valid @RequestBody ApprovalSubmissionRequest request
    ) {
        ApprovalResponse approval = approvalService.submitApproval(inspectionId, request);
        return ResponseEntity.ok(approval);
    }

    /**
     * 담당자 확정 (API) - 결재 없이 DRAFT → CMPLT (PLN/ACT 통합)
     */
    @PostMapping("/{inspectionId}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable String inspectionId) {
        service.onComplete(inspectionId);
        return ResponseEntity.ok().build();
    }

    /**
     * 실적 입력 준비 (API) - 계획 복사 → 실적 생성
     */
    @PostMapping("/{inspectionId}/prepare-actual")
    public ResponseEntity<InspectionResponse> prepareActual(@PathVariable String inspectionId) {
        service.prepareActualStage(inspectionId);
        InspectionResponse response = service.get(inspectionId);
        return ResponseEntity.ok(response);
    }
}

