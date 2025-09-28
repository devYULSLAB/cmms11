package com.cmms11.web;

import com.cmms11.inspection.InspectionRequest;
import com.cmms11.inspection.InspectionResponse;
import com.cmms11.inspection.InspectionService;
import com.cmms11.code.CodeService;
import com.cmms11.domain.site.SiteService;
import com.cmms11.domain.dept.DeptService;
import jakarta.validation.Valid;
import java.util.List;
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
 * 이름: InspectionController
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 예방점검 웹 화면 및 API 엔드포인트를 제공하는 컨트롤러.
 */
@Controller
public class InspectionController {

    private final InspectionService service;
    private final CodeService codeService;
    private final SiteService siteService;
    private final DeptService deptService;

    public InspectionController(InspectionService service, CodeService codeService, SiteService siteService, DeptService deptService) {
        this.service = service;
        this.codeService = codeService;
        this.siteService = siteService;
        this.deptService = deptService;
    }

    // 웹 컨트롤러 화면 제공
    @GetMapping("/inspection/list")
    public String listForm(@RequestParam(name = "inspectionId", required = false) String inspectionId,
                          @RequestParam(name = "plantId", required = false) String plantId,
                          @RequestParam(name = "status", required = false) String status,
                          @RequestParam(name = "plannedDateFrom", required = false) String plannedDateFrom,
                          @RequestParam(name = "plannedDateTo", required = false) String plannedDateTo,
                          Pageable pageable, Model model) {
        Page<InspectionResponse> page = service.list(inspectionId, plantId, status, plannedDateFrom, plannedDateTo, pageable);
        model.addAttribute("page", page);
        model.addAttribute("inspectionId", inspectionId);
        model.addAttribute("plantId", plantId);
        model.addAttribute("status", status);
        model.addAttribute("plannedDateFrom", plannedDateFrom);
        model.addAttribute("plannedDateTo", plannedDateTo);
        return "inspection/list";
    }

    @GetMapping("/inspection/form")
    public String newForm(Model model) {
        model.addAttribute("inspection", emptyInspection());
        model.addAttribute("isNew", true);
        addReferenceData(model);
        return "inspection/form";
    }

    @GetMapping("/inspection/plan")
    public String planForm(Model model) {
        addReferenceData(model);
        return "inspection/plan";
    }

    @GetMapping("/inspection/detail/{inspectionId}")
    public String detailForm(@PathVariable String inspectionId, Model model) {
        InspectionResponse inspection = service.get(inspectionId);
        model.addAttribute("inspection", inspection);
        return "inspection/detail";
    }

    @GetMapping("/inspection/edit/{inspectionId}")
    public String editForm(@PathVariable String inspectionId, Model model) {
        InspectionResponse inspection = service.get(inspectionId);
        model.addAttribute("inspection", inspection);
        model.addAttribute("isNew", false);
        addReferenceData(model);
        return "inspection/form";
    }

    @PostMapping("/inspection/save")
    public String saveForm(@ModelAttribute InspectionRequest request, @RequestParam(required = false) String isNew) {
        if ("true".equals(isNew)) {
            service.create(request);
        } else {
            service.update(request.inspectionId(), request);
        }
        return "redirect:/inspection/list";
    }

    @PostMapping("/inspection/delete/{inspectionId}")
    public String deleteForm(@PathVariable String inspectionId) {
        service.delete(inspectionId);
        return "redirect:/inspection/list";
    }

    // API 엔드포인트 제공
    @ResponseBody
    @GetMapping("/api/inspections")
    public Page<InspectionResponse> list(@RequestParam(name = "inspectionId", required = false) String inspectionId,
                                        @RequestParam(name = "plantId", required = false) String plantId,
                                        @RequestParam(name = "status", required = false) String status,
                                        @RequestParam(name = "plannedDateFrom", required = false) String plannedDateFrom,
                                        @RequestParam(name = "plannedDateTo", required = false) String plannedDateTo,
                                        Pageable pageable) {
        return service.list(inspectionId, plantId, status, plannedDateFrom, plannedDateTo, pageable);
    }

    @ResponseBody
    @GetMapping("/api/inspections/{inspectionId}")
    public ResponseEntity<InspectionResponse> get(@PathVariable String inspectionId) {
        return ResponseEntity.ok(service.get(inspectionId));
    }

    @ResponseBody
    @PostMapping("/api/inspections")
    public ResponseEntity<InspectionResponse> create(@Valid @RequestBody InspectionRequest request) {
        InspectionResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ResponseBody
    @PutMapping("/api/inspections/{inspectionId}")
    public ResponseEntity<InspectionResponse> update(
        @PathVariable String inspectionId,
        @Valid @RequestBody InspectionRequest request
    ) {
        InspectionResponse response = service.update(inspectionId, request);
        return ResponseEntity.ok(response);
    }

    @ResponseBody
    @DeleteMapping("/api/inspections/{inspectionId}")
    public ResponseEntity<Void> delete(@PathVariable String inspectionId) {
        service.delete(inspectionId);
        return ResponseEntity.noContent().build();
    }

    private InspectionResponse emptyInspection() {
        return new InspectionResponse(
            null, // inspectionId
            null, // name
            null, // plantId
            null, // jobId
            null, // siteId
            null, // deptId
            null, // memberId
            null, // plannedDate
            null, // actualDate
            null, // status
            null, // fileGroupId
            null, // note
            null, // createdAt
            null, // createdBy
            null, // updatedAt
            null, // updatedBy
            null  // items
        );
    }

    private void addReferenceData(Model model) {
        // 작업유형 (JOBTP 코드 타입)
        try {
            model.addAttribute("jobTypes", codeService.listItems("JOBTP", null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("jobTypes", List.of());
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
    }
}
