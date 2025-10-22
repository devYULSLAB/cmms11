package com.cmms11.web.api;

import com.cmms11.common.upload.BulkUploadPreview;
import com.cmms11.common.upload.BulkUploadResult;
import com.cmms11.inventory.InventoryRequest;
import com.cmms11.inventory.InventoryResponse;
import com.cmms11.inventory.InventoryService;
import com.cmms11.inventory.InventoryUploadDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * 이름: InventoryApiController
 * 작성자: codex
 * 작성일: 2025-10-15
 * 프로그램 개요: 재고 마스터 관리 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/inventories")
public class InventoryApiController {

    private final InventoryService service;

    public InventoryApiController(InventoryService service) {
        this.service = service;
    }

    /**
     * 재고 목록 조회 (API) - picker용
     */
    @GetMapping
    public ResponseEntity<Page<InventoryResponse>> list(
        @RequestParam(name = "inventoryId", required = false) String inventoryId,
        @RequestParam(name = "name", required = false) String name,
        @RequestParam(name = "makerName", required = false) String makerName,
        @RequestParam(name = "deptId", required = false) String deptId,
        Pageable pageable
    ) {
        Page<InventoryResponse> page = service.list(inventoryId, name, makerName, deptId, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * 재고 단건 조회 (API)
     */
    @GetMapping("/{inventoryId}")
    public ResponseEntity<InventoryResponse> get(@PathVariable String inventoryId) {
        InventoryResponse response = service.get(inventoryId);
        return ResponseEntity.ok(response);
    }

    /**
     * 재고 생성 (API)
     */
    @PostMapping
    public ResponseEntity<InventoryResponse> create(@Valid @RequestBody InventoryRequest request) {
        InventoryResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 재고 수정 (API)
     */
    @PutMapping("/{inventoryId}")
    public ResponseEntity<InventoryResponse> update(
        @PathVariable String inventoryId,
        @Valid @RequestBody InventoryRequest request
    ) {
        InventoryResponse response = service.update(inventoryId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 재고 삭제 (API)
     */
    @DeleteMapping("/{inventoryId}")
    public ResponseEntity<Void> delete(@PathVariable String inventoryId) {
        service.delete(inventoryId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 자재/재고 대량 업로드 - 검증만 수행 (저장하지 않음)
     * 유효한 데이터와 오류 내역을 반환
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkUploadPreview<InventoryUploadDto>> upload(
        @RequestParam("file") MultipartFile file
    ) {
        BulkUploadPreview<InventoryUploadDto> preview = service.validateUpload(file);
        return ResponseEntity.ok(preview);
    }

    /**
     * 검증된 자재/재고 데이터 일괄 저장
     * 프론트에서 미리보기 확인 후 호출
     */
    @PostMapping("/upload/confirm")
    public ResponseEntity<BulkUploadResult> confirmUpload(
        @RequestBody List<InventoryUploadDto> items
    ) {
        BulkUploadResult result = service.saveUploadedData(items);
        return ResponseEntity.ok(result);
    }
}

