package com.cmms11.web;

import com.cmms11.workpermit.WorkPermitRequest;
import com.cmms11.workpermit.WorkPermitResponse;
import com.cmms11.workpermit.WorkPermitService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이름: WorkPermitController
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 작업허가 웹 화면 및 API 엔드포인트를 제공하는 컨트롤러.
 */
@Controller
public class WorkPermitController {

    private final WorkPermitService service;
    private final CodeService codeService;
    private final SiteService siteService;
    private final DeptService deptService;

    public WorkPermitController(WorkPermitService service, CodeService codeService, SiteService siteService, DeptService deptService) {
        this.service = service;
        this.codeService = codeService;
        this.siteService = siteService;
        this.deptService = deptService;
    }

    // 웹 컨트롤러 화면 제공
    @GetMapping("/workpermit/list")
    public String listForm(@RequestParam(name = "permitId", required = false) String permitId,
                          @RequestParam(name = "plantId", required = false) String plantId,
                          @RequestParam(name = "jobId", required = false) String jobId,
                          @RequestParam(name = "status", required = false) String status,
                          @RequestParam(name = "plannedDateFrom", required = false) String plannedDateFrom,
                          Pageable pageable, Model model) {
        Page<WorkPermitResponse> page = service.list(permitId, plantId, jobId, status, plannedDateFrom, pageable);
        model.addAttribute("page", page);
        model.addAttribute("permitId", permitId);
        model.addAttribute("plantId", plantId);
        model.addAttribute("jobId", jobId);
        model.addAttribute("status", status);
        model.addAttribute("plannedDateFrom", plannedDateFrom);
        return "workpermit/list";
    }

    @GetMapping("/workpermit/form")
    public String newForm(Model model) {
        model.addAttribute("workPermit", emptyWorkPermit());
        model.addAttribute("isNew", true);
        addReferenceData(model);
        return "workpermit/form";
    }

    @GetMapping("/workpermit/edit/{workPermitId}")
    public String editForm(@PathVariable String workPermitId, Model model) {
        WorkPermitResponse workPermit = service.get(workPermitId);
        model.addAttribute("workPermit", workPermit);
        model.addAttribute("isNew", false);
        addReferenceData(model);
        return "workpermit/form";
    }

    @GetMapping("/workpermit/detail/{workPermitId}")
    public String detailForm(@PathVariable String workPermitId, Model model) {
        WorkPermitResponse workPermit = service.get(workPermitId);
        model.addAttribute("workpermit", workPermit);
        return "workpermit/detail";
    }

    @PostMapping("/workpermit/save")
    public String saveForm(@ModelAttribute WorkPermitRequest request, @RequestParam(required = false) String isNew) {
        if ("true".equals(isNew)) {
            service.create(request);
        } else {
            service.update(request.permitId(), request);
        }
        return "redirect:/workpermit/list";
    }

    @PostMapping("/workpermit/delete/{workPermitId}")
    public String deleteForm(@PathVariable String workPermitId) {
        service.delete(workPermitId);
        return "redirect:/workpermit/list";
    }

    // API 엔드포인트 제공
    @ResponseBody
    @GetMapping("/api/workpermits")
    public Page<WorkPermitResponse> list(@RequestParam(name = "permitId", required = false) String permitId,
                                        @RequestParam(name = "plantId", required = false) String plantId,
                                        @RequestParam(name = "jobId", required = false) String jobId,
                                        @RequestParam(name = "status", required = false) String status,
                                        @RequestParam(name = "plannedDateFrom", required = false) String plannedDateFrom,
                                        Pageable pageable) {
        return service.list(permitId, plantId, jobId, status, plannedDateFrom, pageable);
    }

    @ResponseBody
    @GetMapping("/api/workpermits/{workPermitId}")
    public ResponseEntity<WorkPermitResponse> get(@PathVariable String workPermitId) {
        return ResponseEntity.ok(service.get(workPermitId));
    }

    @ResponseBody
    @PostMapping("/api/workpermits")
    public ResponseEntity<WorkPermitResponse> create(@Valid @RequestBody WorkPermitRequest request) {
        WorkPermitResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ResponseBody
    @PutMapping("/api/workpermits/{workPermitId}")
    public ResponseEntity<WorkPermitResponse> update(
        @PathVariable String workPermitId,
        @Valid @RequestBody WorkPermitRequest request
    ) {
        WorkPermitResponse response = service.update(workPermitId, request);
        return ResponseEntity.ok(response);
    }

    @ResponseBody
    @DeleteMapping("/api/workpermits/{workPermitId}")
    public ResponseEntity<Void> delete(@PathVariable String workPermitId) {
        service.delete(workPermitId);
        return ResponseEntity.noContent().build();
    }

    private WorkPermitResponse emptyWorkPermit() {
        return new WorkPermitResponse(
            null, // workPermitId
            null, // name
            null, // plantId
            null, // jobId
            null, // siteId
            null, // deptId
            null, // memberId
            null, // plannedDate
            null, // actualDate
            null, // workSummary
            null, // hazardFactor
            null, // safetyFactor
            null, // checksheetJson
            "REQ", // status - 기본값 설정
            null, // fileGroupId
            null, // note
            null, // createdAt
            null, // createdBy
            null, // updatedAt
            null  // updatedBy
        );
    }

    private void addReferenceData(Model model) {
        // 허가유형 (PERMT 코드 타입)
        try {
            model.addAttribute("permitTypes", codeService.listItems("PERMT", null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("permitTypes", List.of());
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
