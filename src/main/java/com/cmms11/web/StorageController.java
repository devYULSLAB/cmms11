package com.cmms11.web;

import com.cmms11.domain.storage.StorageRequest;
import com.cmms11.domain.storage.StorageResponse;
import com.cmms11.domain.storage.StorageService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 이름: StorageController
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 창고(Storage) 기준정보 웹 화면 및 API 엔드포인트를 제공하는 컨트롤러.
 */
@Controller
public class StorageController {

    private final StorageService service;

    public StorageController(StorageService service) {
        this.service = service;
    }

    // 웹 컨트롤러 화면 제공
    @GetMapping("/domain/storage/list")
    public String listForm(@RequestParam(name = "q", required = false) String q, Pageable pageable, Model model) {
        Page<StorageResponse> page = service.list(q, pageable);
        model.addAttribute("page", page);
        model.addAttribute("keyword", q);
        return "domain/storage/list";
    }

    @GetMapping("/domain/storage/form")
    public String newForm(Model model) {
        model.addAttribute("storage", emptyStorage());
        model.addAttribute("isNew", true);
        return "domain/storage/form";
    }

    @GetMapping("/domain/storage/edit/{storageId}")
    public String editForm(@PathVariable String storageId, Model model) {
        StorageResponse storage = service.get(storageId);
        model.addAttribute("storage", storage);
        model.addAttribute("isNew", false);
        return "domain/storage/form";
    }

    @PostMapping("/domain/storage/save")
    public String save(@ModelAttribute StorageRequest request, @RequestParam(required = false) String isNew) {
        if ("true".equals(isNew)) {
            service.create(request);
        } else {
            service.update(request.storageId(), request);
        }
        return "redirect:/layout/defaultLayout.html?content=/domain/storage/list";
    }

    @PostMapping("/domain/storage/delete/{storageId}")
    public String deleteForm(@PathVariable String storageId) {
        service.delete(storageId);
        return "redirect:/layout/defaultLayout.html?content=/domain/storage/list";
    }

    // API 엔드포인트 제공
    // @ResponseBody
    // @GetMapping("/api/domain/storages")
    // public Page<StorageResponse> list(@RequestParam(name = "q", required = false) String q, Pageable pageable) {
    //     return service.list(q, pageable);
    // }

    // @ResponseBody
    // @GetMapping("/api/domain/storages/{storageId}")
    // public ResponseEntity<StorageResponse> get(@PathVariable String storageId) {
    //     return ResponseEntity.ok(service.get(storageId));
    // }

    // @ResponseBody
    // @PostMapping("/api/domain/storages")
    // public ResponseEntity<StorageResponse> create(@Valid @RequestBody StorageRequest request) {
    //     StorageResponse response = service.create(request);
    //     return ResponseEntity.status(HttpStatus.CREATED).body(response);
    // }

    // @ResponseBody
    // @PutMapping("/api/domain/storages/{storageId}")
    // public ResponseEntity<StorageResponse> update(
    //     @PathVariable String storageId,
    //     @Valid @RequestBody StorageRequest request
    // ) {
    //     StorageResponse response = service.update(storageId, request);
    //     return ResponseEntity.ok(response);
    // }

    // @ResponseBody
    // @DeleteMapping("/api/domain/storages/{storageId}")
    // public ResponseEntity<Void> delete(@PathVariable String storageId) {
    //     service.delete(storageId);
    //     return ResponseEntity.noContent().build();
    // }

    private StorageResponse emptyStorage() {
        return new StorageResponse(
            null,
            null,
            null,
            "N",
            null,
            null,
            null,
            null
        );
    }
}
