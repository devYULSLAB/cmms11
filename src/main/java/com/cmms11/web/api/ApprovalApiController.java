package com.cmms11.web.api;

import com.cmms11.approval.ApprovalRequest;
import com.cmms11.approval.ApprovalResponse;
import com.cmms11.approval.ApprovalService;
import jakarta.validation.Valid;
import java.util.Map;
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
 * 이름: ApprovalApiController
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 결재 관리 API 컨트롤러 (JSON 반환)
 */
@RestController
@RequestMapping("/api/approvals")
public class ApprovalApiController {

    private final ApprovalService service;

    public ApprovalApiController(ApprovalService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<ApprovalResponse>> list(
        @RequestParam(required = false) String status,
        Pageable pageable
    ) {
        Page<ApprovalResponse> page = service.list(status, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{approvalId}")
    public ResponseEntity<ApprovalResponse> get(@PathVariable String approvalId) {
        ApprovalResponse response = service.get(approvalId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApprovalResponse> create(@Valid @RequestBody ApprovalRequest request) {
        ApprovalResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{approvalId}")
    public ResponseEntity<ApprovalResponse> update(
        @PathVariable String approvalId,
        @Valid @RequestBody ApprovalRequest request
    ) {
        ApprovalResponse response = service.update(approvalId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{approvalId}")
    public ResponseEntity<Void> delete(@PathVariable String approvalId) {
        service.delete(approvalId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 결재 승인 (원본 모듈 콜백 호출)
     */
    @PostMapping("/{approvalId}/approve")
    public ResponseEntity<ApprovalResponse> approve(
        @PathVariable String approvalId,
        @RequestBody(required = false) Map<String, String> body
    ) {
        String comment = body != null ? body.get("comment") : null;
        ApprovalResponse response = service.approve(approvalId, comment);
        return ResponseEntity.ok(response);
    }

    /**
     * 결재 반려 (원본 모듈 콜백 호출)
     */
    @PostMapping("/{approvalId}/reject")
    public ResponseEntity<ApprovalResponse> reject(
        @PathVariable String approvalId,
        @RequestBody(required = false) Map<String, String> body
    ) {
        String comment = body != null ? body.get("comment") : null;
        ApprovalResponse response = service.reject(approvalId, comment);
        return ResponseEntity.ok(response);
    }
}

