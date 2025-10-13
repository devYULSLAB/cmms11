package com.cmms11.web.api;

import com.cmms11.memo.MemoRequest;
import com.cmms11.memo.MemoResponse;
import com.cmms11.memo.MemoService;
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
 * 이름: MemoApiController
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 메모/게시글 관리 API 컨트롤러 (JSON 반환)
 * 참고: Memo는 단순 CRUD만 있고, 복잡한 워크플로우는 없음
 */
@RestController
@RequestMapping("/api/memos")
public class MemoApiController {

    private final MemoService service;

    public MemoApiController(MemoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<MemoResponse>> list(
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String createdBy,
        @RequestParam(required = false) String refEntity,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String stage,
        Pageable pageable
    ) {
        Page<MemoResponse> page = service.list(
            title, createdBy, refEntity, status, stage, pageable
        );
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{memoId}")
    public ResponseEntity<MemoResponse> get(@PathVariable String memoId) {
        MemoResponse response = service.get(memoId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<MemoResponse> create(@Valid @RequestBody MemoRequest request) {
        MemoResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{memoId}")
    public ResponseEntity<MemoResponse> update(
        @PathVariable String memoId,
        @Valid @RequestBody MemoRequest request
    ) {
        MemoResponse response = service.update(memoId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{memoId}")
    public ResponseEntity<Void> delete(@PathVariable String memoId) {
        service.delete(memoId);
        return ResponseEntity.noContent().build();
    }
}

