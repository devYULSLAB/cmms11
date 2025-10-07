package com.cmms11.web;

import com.cmms11.common.upload.BulkUploadResult;
import com.cmms11.inventory.InventoryRequest;
import com.cmms11.inventory.InventoryResponse;
import com.cmms11.inventory.InventoryService;
import com.cmms11.code.CodeService;
import com.cmms11.domain.dept.DeptService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이름: InventoryController
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 재고 마스터 웹 화면 및 API 엔드포인트를 제공하는 컨트롤러.
 */
@Controller
public class InventoryController {

    private final InventoryService service;
    private final CodeService codeService;
    private final DeptService deptService;

    public InventoryController(InventoryService service, CodeService codeService, DeptService deptService) {
        this.service = service;
        this.codeService = codeService;
        this.deptService = deptService;
    }

    // 웹 컨트롤러 화면 제공
    @GetMapping("/inventory/list")
    public String listForm(@RequestParam(name = "inventoryId", required = false) String inventoryId,
                          @RequestParam(name = "name", required = false) String name,
                          @RequestParam(name = "makerName", required = false) String makerName,
                          @RequestParam(name = "deptId", required = false) String deptId,
                          Pageable pageable, Model model) {
        Page<InventoryResponse> page = service.list(inventoryId, name, makerName, deptId, pageable);
        model.addAttribute("page", page);
        model.addAttribute("inventoryId", inventoryId);
        model.addAttribute("name", name);
        model.addAttribute("makerName", makerName);
        model.addAttribute("deptId", deptId);
        // 부서 목록 추가
        model.addAttribute("depts", deptService.list(null, Pageable.unpaged()).getContent());
        return "inventory/list";
    }

    // 업로드 폼 화면 제공 (미구현 - API 활용할 것)
    @GetMapping("/inventory/uploadForm")
    public String uploadForm(Model model) {
        return "inventory/uploadForm";
    }

    @GetMapping("/inventory/form")
    public String newForm(Model model) {
        model.addAttribute("inventory", emptyInventory());
        model.addAttribute("isNew", true);
        addReferenceData(model);
        return "inventory/form";
    }

    @GetMapping("/inventory/detail/{inventoryId}")
    public String detailForm(@PathVariable String inventoryId, Model model) {
        InventoryResponse inventory = service.get(inventoryId);
        model.addAttribute("inventory", inventory);
        return "inventory/detail";
    }

    @GetMapping("/inventory/edit/{inventoryId}")
    public String editForm(@PathVariable String inventoryId, Model model) {
        InventoryResponse inventory = service.get(inventoryId);
        model.addAttribute("inventory", inventory);
        model.addAttribute("isNew", false);
        addReferenceData(model);
        return "inventory/form";
    }

    @PostMapping("/inventory/save")
    public String saveForm(@ModelAttribute InventoryRequest request, @RequestParam(required = false) String isNew) {
        if ("true".equals(isNew)) {
            service.create(request);
        } else {
            service.update(request.inventoryId(), request);
        }
        return "redirect:/inventory/list";
    }

    @PostMapping("/inventory/delete/{inventoryId}")
    public String deleteForm(@PathVariable String inventoryId) {
        service.delete(inventoryId);
        return "redirect:/inventory/list";
    }

    // API 엔드포인트 제공
    @ResponseBody
    @GetMapping("/api/inventories")
    public Page<InventoryResponse> list(@RequestParam(name = "inventoryId", required = false) String inventoryId,
                                       @RequestParam(name = "name", required = false) String name,
                                       @RequestParam(name = "makerName", required = false) String makerName,
                                       @RequestParam(name = "deptId", required = false) String deptId,
                                       Pageable pageable) {
        return service.list(inventoryId, name, makerName, deptId, pageable);
    }

    @ResponseBody
    @GetMapping("/api/inventories/{inventoryId}")
    public ResponseEntity<InventoryResponse> get(@PathVariable String inventoryId) {
        return ResponseEntity.ok(service.get(inventoryId));
    }

    @ResponseBody
    @PostMapping("/api/inventories")
    public ResponseEntity<InventoryResponse> create(@Valid @RequestBody InventoryRequest request) {
        InventoryResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ResponseBody
    @PutMapping("/api/inventories/{inventoryId}")
    public ResponseEntity<InventoryResponse> update(
        @PathVariable String inventoryId,
        @Valid @RequestBody InventoryRequest request
    ) {
        InventoryResponse response = service.update(inventoryId, request);
        return ResponseEntity.ok(response);
    }

    @ResponseBody
    @DeleteMapping("/api/inventories/{inventoryId}")
    public ResponseEntity<Void> delete(@PathVariable String inventoryId) {
        service.delete(inventoryId);
        return ResponseEntity.noContent().build();
    }

    @ResponseBody
    @PostMapping(value = "/api/inventories/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkUploadResult> upload(@RequestParam("file") MultipartFile file) {
        BulkUploadResult result = service.upload(file);
        return ResponseEntity.ok(result);
    }

    private InventoryResponse emptyInventory() {
        return new InventoryResponse(
            null, // inventoryId
            null, // name
            null, // assetId
            null, // deptId
            null, // makerName
            null, // spec
            null, // model
            null, // serial
            null, // fileGroupId
            null, // note
            "ACTIVE", // status
            "N", // deleteMark
            null, // createdAt
            null, // createdBy
            null, // updatedAt
            null  // updatedBy
        );
    }

    private void addReferenceData(Model model) {
        // 자산유형 (ASSET 코드 타입)
        try {
            model.addAttribute("assetTypes", codeService.listItems("ASSET", null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("assetTypes", List.of());
        }

        // 부서 목록
        try {
            model.addAttribute("depts", deptService.list(null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("depts", List.of());
        }
    }
}
