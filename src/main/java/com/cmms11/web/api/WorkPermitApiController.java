package com.cmms11.web.api;

import com.cmms11.approval.ApprovalResponse;
import com.cmms11.workpermit.WorkPermitRequest;
import com.cmms11.workpermit.WorkPermitResponse;
import com.cmms11.workpermit.WorkPermitService;
import jakarta.validation.Valid;
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
 * 이름: WorkPermitApiController
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 작업허가 관리 API 컨트롤러 (JSON 반환)
 * 참고: WorkPermit은 실적 입력이 없으므로 계획(PLN) 워크플로우만 존재
 */
@RestController
@RequestMapping("/api/workpermits")
public class WorkPermitApiController {

    private final WorkPermitService service;

    public WorkPermitApiController(WorkPermitService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<WorkPermitResponse>> list(
        @RequestParam(required = false) String permitId,
        @RequestParam(required = false) String plantId,
        @RequestParam(required = false) String jobId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String stage,
        @RequestParam(required = false) String plannedDateFrom,
        Pageable pageable
    ) {
        Page<WorkPermitResponse> page = service.list(
            permitId, plantId, jobId, status, stage, plannedDateFrom, pageable
        );
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{permitId}")
    public ResponseEntity<WorkPermitResponse> get(@PathVariable String permitId) {
        WorkPermitResponse response = service.get(permitId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WorkPermitResponse> create(@Valid @RequestBody WorkPermitRequest request) {
        WorkPermitResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{permitId}")
    public ResponseEntity<WorkPermitResponse> update(
        @PathVariable String permitId,
        @Valid @RequestBody WorkPermitRequest request
    ) {
        WorkPermitResponse response = service.update(permitId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{permitId}")
    public ResponseEntity<Void> delete(@PathVariable String permitId) {
        service.delete(permitId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{permitId}/submit-plan-approval")
    public ResponseEntity<ApprovalResponse> submitPlanApproval(@PathVariable String permitId) {
        ApprovalResponse approval = service.submitPlanApproval(permitId);
        return ResponseEntity.ok(approval);
    }

    @PostMapping("/{permitId}/confirm-plan")
    public ResponseEntity<Void> confirmPlan(@PathVariable String permitId) {
        service.onPlanApprovalComplete(permitId);
        return ResponseEntity.ok().build();
    }
}

