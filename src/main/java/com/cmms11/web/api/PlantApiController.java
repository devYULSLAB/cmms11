package com.cmms11.web.api;

import com.cmms11.common.upload.BulkUploadResult;
import com.cmms11.plant.PlantRequest;
import com.cmms11.plant.PlantResponse;
import com.cmms11.plant.PlantService;
import jakarta.validation.Valid;
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
 * 이름: PlantApiController
 * 작성자: codex
 * 작성일: 2025-10-15
 * 프로그램 개요: 설비 관리 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/plants")
public class PlantApiController {

    private final PlantService service;

    public PlantApiController(PlantService service) {
        this.service = service;
    }

    /**
     * 설비 목록 조회 (API) - picker용
     */
    @GetMapping
    public ResponseEntity<Page<PlantResponse>> list(
        @RequestParam(name = "plantId", required = false) String plantId,
        @RequestParam(name = "name", required = false) String name,
        @RequestParam(name = "makerName", required = false) String makerName,
        @RequestParam(name = "funcId", required = false) String funcId,
        Pageable pageable
    ) {
        Page<PlantResponse> page = service.list(plantId, name, makerName, funcId, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * 설비 단건 조회 (API)
     */
    @GetMapping("/{plantId}")
    public ResponseEntity<PlantResponse> get(@PathVariable String plantId) {
        PlantResponse response = service.get(plantId);
        return ResponseEntity.ok(response);
    }

    /**
     * 설비 생성 (API)
     */
    @PostMapping
    public ResponseEntity<PlantResponse> create(@Valid @RequestBody PlantRequest request) {
        PlantResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 설비 수정 (API)
     */
    @PutMapping("/{plantId}")
    public ResponseEntity<PlantResponse> update(
        @PathVariable String plantId,
        @Valid @RequestBody PlantRequest request
    ) {
        PlantResponse response = service.update(plantId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 설비 삭제 (API)
     */
    @DeleteMapping("/{plantId}")
    public ResponseEntity<Void> delete(@PathVariable String plantId) {
        service.delete(plantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 설비 대량 업로드 (API)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkUploadResult> upload(@RequestParam("file") MultipartFile file) {
        BulkUploadResult result = service.upload(file);
        return ResponseEntity.ok(result);
    }
}

