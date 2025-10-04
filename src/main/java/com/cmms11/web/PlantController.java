package com.cmms11.web;

import com.cmms11.common.upload.BulkUploadResult;
import com.cmms11.plant.Plant;
import com.cmms11.plant.PlantRequest;
import com.cmms11.plant.PlantResponse;
import com.cmms11.plant.PlantService;
import com.cmms11.code.CodeService;
import com.cmms11.domain.site.SiteService;
import com.cmms11.domain.dept.DeptService;
import com.cmms11.domain.func.FuncService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이름: PlantController
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 설비 웹 화면 및 API 엔드포인트를 제공하는 컨트롤러.
 */
@Controller
public class PlantController {
    private final PlantService service;
    private final CodeService codeService;
    private final SiteService siteService;
    private final DeptService deptService;
    private final FuncService funcService;

    public PlantController(PlantService service, CodeService codeService, SiteService siteService, DeptService deptService, FuncService funcService) {
        this.service = service;
        this.codeService = codeService;
        this.siteService = siteService;
        this.deptService = deptService;
        this.funcService = funcService;
    }

    // 웹 컨트롤러 화면 제공
    @GetMapping("/plant/list")
    public String listForm(@RequestParam(name = "plantId", required = false) String plantId,
                          @RequestParam(name = "name", required = false) String name,
                          @RequestParam(name = "makerName", required = false) String makerName,
                          @RequestParam(name = "funcId", required = false) String funcId,
                          Pageable pageable, Model model) {
        Page<PlantResponse> page = service.list(plantId, name, makerName, funcId, pageable);
        model.addAttribute("page", page);
        model.addAttribute("plantId", plantId);
        model.addAttribute("name", name);
        model.addAttribute("makerName", makerName);
        model.addAttribute("funcId", funcId);
        // 부서 목록 추가 (필터용 - 현재는 사용하지 않음)
        model.addAttribute("depts", deptService.list(null, Pageable.unpaged()).getContent());
        return "plant/list";
    }

    @GetMapping("/plant/form")
    public String newForm(Model model) {
        model.addAttribute("plant", emptyPlant());
        model.addAttribute("isNew", true);
        addReferenceData(model);
        return "plant/form";
    }

    @GetMapping("/plant/uploadForm")
    public String uploadForm(Model model) {
        return "plant/uploadForm";
    }

    @GetMapping("/plant/history")
    public String historyForm(@RequestParam(name = "plantId", required = false) String plantId,
                             @RequestParam(name = "plantName", required = false) String plantName,
                             Model model) {
        model.addAttribute("plantId", plantId);
        model.addAttribute("plantName", plantName);
        return "plant/history";
    }

    @GetMapping("/plant/edit/{plantId}")
    public String editForm(@PathVariable String plantId, Model model) {
        PlantResponse plant = service.get(plantId);
        model.addAttribute("plant", plant);
        model.addAttribute("isNew", false);
        addReferenceData(model);
        return "plant/form";
    }

    @GetMapping("/plant/detail/{plantId}")
    public String detailForm(@PathVariable String plantId, Model model) {
        PlantResponse plant = service.get(plantId);
        model.addAttribute("plant", plant);
        return "plant/detail";
    }

    @PostMapping("/plant/save")
    public String saveForm(@ModelAttribute PlantRequest request, @RequestParam(required = false) String isNew) {
        if ("true".equals(isNew)) {
            service.create(request);
        } else {
            service.update(request.plantId(), request);
        }
        return "redirect:/plant/list";
    }

    @PostMapping("/plant/delete/{plantId}")
    public String deleteForm(@PathVariable String plantId) {
        service.delete(plantId);
        return "redirect:/plant/list";
    }

    // API 엔드포인트 제공
    @ResponseBody
    @GetMapping("/api/plants")
    public Page<PlantResponse> list(@RequestParam(name = "plantId", required = false) String plantId,
                                    @RequestParam(name = "name", required = false) String name,
                                    @RequestParam(name = "makerName", required = false) String makerName,
                                    @RequestParam(name = "funcId", required = false) String funcId,
                                    Pageable pageable) {
        return service.list(plantId, name, makerName, funcId, pageable);
    }

    @ResponseBody
    @GetMapping("/api/plants/{plantId}")
    public ResponseEntity<PlantResponse> get(@PathVariable String plantId) {
        return ResponseEntity.ok(service.get(plantId));
    }

    @ResponseBody
    @PostMapping("/api/plants")
    public ResponseEntity<PlantResponse> create(@Valid @RequestBody PlantRequest request) {
        PlantResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ResponseBody
    @PutMapping("/api/plants/{plantId}")
    public ResponseEntity<PlantResponse> update(@PathVariable String plantId, @Valid @RequestBody PlantRequest request) {
        PlantResponse response = service.update(plantId, request);
        return ResponseEntity.ok(response);
    }

    @ResponseBody
    @DeleteMapping("/api/plants/{plantId}")
    public ResponseEntity<Void> delete(@PathVariable String plantId) {
        service.delete(plantId);
        return ResponseEntity.noContent().build();
    }

    @ResponseBody
    @PostMapping(value = "/api/plants/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkUploadResult> upload(@RequestParam("file") MultipartFile file) {
        BulkUploadResult result = service.upload(file);
        return ResponseEntity.ok(result);
    }

    private PlantResponse emptyPlant() {
        return new PlantResponse(
            null, // plantId
            null, // name
            null, // assetId
            null, // siteId
            null, // deptId
            null, // funcId
            null, // makerName
            null, // spec
            null, // model
            null, // serial
            null, // installDate
            null, // depreId
            null, // deprePeriod
            null, // purchaseCost
            null, // residualValue
            "N", // inspectionYn
            "N", // psmYn
            "N", // workpermitYn
            null, // inspectionInterval
            null, // lastInspection
            null, // nextInspection
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

        // 감가유형 (DEPRE 코드 타입)
        try {
            model.addAttribute("depreTypes", codeService.listItems("DEPRE", null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("depreTypes", List.of());
        }

        // 사업장 목록
        try {
            model.addAttribute("sites", siteService.list(null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("sites", List.of());
        }

        // 부서 목록
        try {
            model.addAttribute("depts", deptService.list(null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("depts", List.of());
        }

        // 기능위치 목록
        try {
            model.addAttribute("funcs", funcService.list(null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("funcs", List.of());
        }
    }
}
