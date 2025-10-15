package com.cmms11.web.api;

import com.cmms11.approval.ApprovalResponse;
import com.cmms11.workorder.WorkOrderApprovalFacade;
import com.cmms11.workorder.WorkOrderItem;
import com.cmms11.workorder.WorkOrderRequest;
import com.cmms11.workorder.WorkOrderResponse;
import com.cmms11.workorder.WorkOrderService;
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
 * 이름: WorkOrderApiController
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 작업지시 관리 API 컨트롤러 (JSON 반환)
 */
@RestController
@RequestMapping("/api/workorders")
public class WorkOrderApiController {

    private final WorkOrderService service;
    private final WorkOrderApprovalFacade approvalFacade;

    public WorkOrderApiController(WorkOrderService service, WorkOrderApprovalFacade approvalFacade) {
        this.service = service;
        this.approvalFacade = approvalFacade;
    }

    @GetMapping
    public ResponseEntity<Page<WorkOrderResponse>> list(
        @RequestParam(required = false) String orderId,
        @RequestParam(required = false) String plantId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String stage,
        @RequestParam(required = false) String plannedDateFrom,
        @RequestParam(required = false) String plannedDateTo,
        Pageable pageable
    ) {
        Page<WorkOrderResponse> page = service.list(
            orderId, plantId, status, stage, plannedDateFrom, plannedDateTo, pageable
        );
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<WorkOrderResponse> get(@PathVariable String orderId) {
        WorkOrderResponse response = service.get(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}/items")
    public ResponseEntity<List<WorkOrderItem>> getItems(@PathVariable String orderId) {
        List<WorkOrderItem> items = service.getItems(orderId);
        return ResponseEntity.ok(items);
    }

    @PostMapping
    public ResponseEntity<WorkOrderResponse> create(@Valid @RequestBody WorkOrderRequest request) {
        WorkOrderResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<WorkOrderResponse> update(
        @PathVariable String orderId,
        @Valid @RequestBody WorkOrderRequest request
    ) {
        WorkOrderResponse response = service.update(orderId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> delete(@PathVariable String orderId) {
        service.delete(orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/submit-plan-approval")
    public ResponseEntity<ApprovalResponse> submitPlanApproval(@PathVariable String orderId) {
        ApprovalResponse approval = approvalFacade.submitPlanApproval(orderId);
        return ResponseEntity.ok(approval);
    }

    @PostMapping("/{orderId}/confirm-plan")
    public ResponseEntity<Void> confirmPlan(@PathVariable String orderId) {
        service.onPlanApprovalComplete(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderId}/prepare-actual")
    public ResponseEntity<WorkOrderResponse> prepareActual(@PathVariable String orderId) {
        approvalFacade.prepareActualStage(orderId);
        WorkOrderResponse response = service.get(orderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/submit-actual-approval")
    public ResponseEntity<ApprovalResponse> submitActualApproval(@PathVariable String orderId) {
        ApprovalResponse approval = approvalFacade.submitActualApproval(orderId);
        return ResponseEntity.ok(approval);
    }
}

