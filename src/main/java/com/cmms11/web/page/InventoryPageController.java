package com.cmms11.web.page;

import com.cmms11.inventory.InventoryRequest;
import com.cmms11.inventory.InventoryResponse;
import com.cmms11.inventory.InventoryService;
import com.cmms11.code.CodeService;
import com.cmms11.domain.dept.DeptService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 이름: InventoryPageController
 * 작성자: codex
 * 작성일: 2025-10-15
 * 프로그램 개요: 재고 마스터 관리 페이지 컨트롤러 (HTML 반환)
 */
@Controller
public class InventoryPageController {

    private final InventoryService service;
    private final CodeService codeService;
    private final DeptService deptService;

    public InventoryPageController(
        InventoryService service,
        CodeService codeService,
        DeptService deptService
    ) {
        this.service = service;
        this.codeService = codeService;
        this.deptService = deptService;
    }

    /**
     * 재고 목록 페이지
     */
    @GetMapping("/inventory/list")
    public String list(
        @RequestParam(name = "inventoryId", required = false) String inventoryId,
        @RequestParam(name = "name", required = false) String name,
        @RequestParam(name = "makerName", required = false) String makerName,
        @RequestParam(name = "deptId", required = false) String deptId,
        Pageable pageable,
        Model model
    ) {
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

    /**
     * 재고 등록 폼 페이지
     */
    @GetMapping("/inventory/form")
    public String form(Model model) {
        model.addAttribute("inventory", emptyInventory());
        model.addAttribute("isNew", true);
        addReferenceData(model);
        return "inventory/form";
    }

    /**
     * 재고 수정 폼 페이지
     */
    @GetMapping("/inventory/edit/{inventoryId}")
    public String edit(@PathVariable String inventoryId, Model model) {
        InventoryResponse inventory = service.get(inventoryId);
        model.addAttribute("inventory", inventory);
        model.addAttribute("isNew", false);
        addReferenceData(model);
        return "inventory/form";
    }

    /**
     * 재고 상세 페이지
     */
    @GetMapping("/inventory/detail/{inventoryId}")
    public String detail(@PathVariable String inventoryId, Model model) {
        InventoryResponse inventory = service.get(inventoryId);
        model.addAttribute("inventory", inventory);
        return "inventory/detail";
    }

    /**
     * 재고 업로드 폼 페이지
     */
    @GetMapping("/inventory/uploadForm")
    public String uploadForm(Model model) {
        return "inventory/uploadForm";
    }

    /**
     * 재고 저장 처리 (POST 방식)
     */
    @PostMapping("/inventory/save")
    public String save(
        @ModelAttribute InventoryRequest request,
        @RequestParam(required = false) String isNew
    ) {
        if ("true".equals(isNew)) {
            service.create(request);
        } else {
            service.update(request.inventoryId(), request);
        }
        return "redirect:/inventory/list";
    }

    /**
     * 재고 삭제 처리 (POST 방식)
     */
    @PostMapping("/inventory/delete/{inventoryId}")
    public String delete(@PathVariable String inventoryId) {
        service.delete(inventoryId);
        return "redirect:/inventory/list";
    }

    /**
     * 빈 Inventory 객체 생성 (신규 등록용)
     */
    private InventoryResponse emptyInventory() {
        return new InventoryResponse(
            null, // inventoryId
            null, // name
            null, // unit
            null, // assetId
            null, // deptId
            null, // makerName
            null, // spec
            null, // model
            null, // serial
            null, // fileGroupId
            null, // note
            "N", // deleteMark
            null, // createdAt
            null, // createdBy
            null, // updatedAt
            null  // updatedBy
        );
    }

    /**
     * Select box용 참조 데이터 추가
     */
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

