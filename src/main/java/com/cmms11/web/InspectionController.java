package com.cmms11.web;

import com.cmms11.inspection.InspectionRequest;
import com.cmms11.inspection.InspectionResponse;
import com.cmms11.inspection.InspectionService;
import com.cmms11.code.CodeService;
import com.cmms11.domain.site.SiteService;
import com.cmms11.domain.dept.DeptService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /* 점검 계획 일괄 수립 */

    @GetMapping("/inspection/plan")
    public String planForm(Model model) {
        addReferenceData(model);
        return "inspection/plan";
    }

    @PostMapping("/inspection/plan/save")
    public String savePlan(@RequestParam Map<String, String> params) {
        // inspections[0].name, inspections[0].plantId, inspections[0].status, ...
        // inspections[1].name, inspections[1].plantId, inspections[1].status, ...
        // 이런 형태의 파라미터를 파싱해서 여러 개 저장
        // status는 plan.html의 hidden field에서 "PLAN"으로 주입되어 넘어옴
        
        List<InspectionRequest> inspections = parseInspectionArray(params);
        inspections.forEach(service::create);
        
        return "redirect:/inspection/list";
    }

    private List<InspectionRequest> parseInspectionArray(Map<String, String> params) {
        // inspections[0], inspections[1] ... 파싱 로직
        Map<Integer, Map<String, String>> grouped = new HashMap<>();
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("inspections[")) {
                // inspections[0].name -> index=0, field=name
                int start = key.indexOf('[') + 1;
                int end = key.indexOf(']');
                int index = Integer.parseInt(key.substring(start, end));
                String field = key.substring(end + 2); // ].name -> name
                
                grouped.computeIfAbsent(index, k -> new HashMap<>())
                    .put(field, entry.getValue());
            }
        }
        
        List<InspectionRequest> result = new ArrayList<>();
        for (Map<String, String> fields : grouped.values()) {
            result.add(createInspectionFromMap(fields));
        }
        
        return result;
    }
    
    private InspectionRequest createInspectionFromMap(Map<String, String> fields) {
        // HTML에서 snake_case로 넘어온 필드명을 camelCase로 매핑
        return new InspectionRequest(
            null, // inspectionId - 자동생성
            fields.get("name"),
            fields.get("plant_id"),
            fields.get("job_id"),
            fields.get("site_id"),
            fields.get("dept_id"),
            fields.get("member_id"),
            parseDate(fields.get("planned_date")),
            parseDate(fields.get("actual_date")),
            fields.get("status"),
            null, // fileGroupId
            fields.get("note"),
            new ArrayList<>() // items - 계획 단계에서는 비어있음
        );
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        return LocalDate.parse(dateStr);
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
