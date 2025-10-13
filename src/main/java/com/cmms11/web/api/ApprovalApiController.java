package com.cmms11.web.api;

import com.cmms11.approval.ApprovalInboxResponse;
import com.cmms11.approval.ApprovalRequest;
import com.cmms11.approval.ApprovalResponse;
import com.cmms11.approval.ApprovalService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        Page<ApprovalResponse> page = service.listByStatus(status, pageable);
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

    @PutMapping("/inbox/{inboxId}/read")
    public ResponseEntity<Map<String, Object>> markInboxAsRead(@PathVariable String inboxId) {
        try {
            service.markInboxAsRead(inboxId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "읽음 처리되었습니다."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "읽음 처리 중 오류가 발생했습니다."
            ));
        }
    }

    @GetMapping("/inbox/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        try {
            long count = service.getUnreadInboxCount();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("count", 0));
        }
    }

    @GetMapping("/inbox")
    public ResponseEntity<Page<ApprovalInboxResponse>> getInboxList(
        @RequestParam(required = false) String type,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        try {
            PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "isRead").and(Sort.by(Sort.Direction.DESC, "submittedAt"))
            );
            Page<ApprovalInboxResponse> inboxPage = service.getMyInbox(type, pageRequest);
            return ResponseEntity.ok(inboxPage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/inbox/stats")
    public ResponseEntity<Map<String, Long>> getInboxStats() {
        try {
            Map<String, Long> stats = new HashMap<>();
            stats.put("pending", service.countInboxByType("SUBMT"));
            stats.put("approved", service.countInboxByType("APPRV"));
            stats.put("rejected", service.countInboxByType("REJCT"));
            stats.put("completed", service.countInboxByType("CMPLT"));
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Long> empty = new HashMap<>();
            empty.put("pending", 0L);
            empty.put("approved", 0L);
            empty.put("rejected", 0L);
            empty.put("completed", 0L);
            return ResponseEntity.ok(empty);
        }
    }

    @GetMapping("/{approvalId}/my-inbox")
    public ResponseEntity<ApprovalInboxResponse> getMyInboxByApproval(@PathVariable String approvalId) {
        try {
            Optional<ApprovalInboxResponse> inbox = service.getMyInboxByApproval(approvalId);
            return inbox.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

