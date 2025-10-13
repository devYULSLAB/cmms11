package com.cmms11.web.page;

import com.cmms11.code.CodeService;
import com.cmms11.domain.dept.DeptService;
import com.cmms11.domain.site.SiteService;
import com.cmms11.workpermit.WorkPermitResponse;
import com.cmms11.workpermit.WorkPermitService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 이름: WorkPermitPageController
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 작업허가 관리 페이지 컨트롤러 (HTML 반환)
 */
@Controller
public class WorkPermitPageController {

    private final WorkPermitService service;
    private final CodeService codeService;
    private final SiteService siteService;
    private final DeptService deptService;

    public WorkPermitPageController(
        WorkPermitService service,
        CodeService codeService,
        SiteService siteService,
        DeptService deptService
    ) {
        this.service = service;
        this.codeService = codeService;
        this.siteService = siteService;
        this.deptService = deptService;
    }

    @GetMapping("/workpermit/list")
    public String list(
        @RequestParam(required = false) String permitId,
        @RequestParam(required = false) String plantId,
        @RequestParam(required = false) String jobId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String stage,
        @RequestParam(required = false) String plannedDateFrom,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Pageable pageable,
        Model model
    ) {
        Page<WorkPermitResponse> page = service.list(
            permitId, plantId, jobId, status, stage, plannedDateFrom, pageable
        );
        
        model.addAttribute("page", page);
        model.addAttribute("permitId", permitId);
        model.addAttribute("plantId", plantId);
        model.addAttribute("jobId", jobId);
        model.addAttribute("status", status);
        model.addAttribute("stage", stage);
        model.addAttribute("plannedDateFrom", plannedDateFrom);
        
        return _fragment ? "workpermit/list :: content" : "workpermit/list";
    }

    @GetMapping("/workpermit/detail/{permitId}")
    public String detail(
        @PathVariable String permitId,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Model model
    ) {
        WorkPermitResponse workPermit = service.get(permitId);
        model.addAttribute("workPermit", workPermit);
        
        return _fragment ? "workpermit/detail :: content" : "workpermit/detail";
    }

    @GetMapping("/workpermit/form")
    public String form(
        @RequestParam(required = false) String id,
        @RequestParam(required = false) String stage,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Model model
    ) {
        boolean isNew = (id == null || id.isEmpty());
        WorkPermitResponse workPermit = isNew ? null : service.get(id);
        
        model.addAttribute("workPermit", workPermit);
        model.addAttribute("isNew", isNew);
        model.addAttribute("stage", stage != null ? stage : "PLN");
        
        addReferenceData(model);
        
        return _fragment ? "workpermit/form :: content" : "workpermit/form";
    }

    private void addReferenceData(Model model) {
        try {
            model.addAttribute("jobTypes", codeService.listItems("JOBTP", null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("jobTypes", java.util.List.of());
        }
        
        try {
            model.addAttribute("permitTypes", codeService.listItems("PERMT", null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("permitTypes", java.util.List.of());
        }
        
        try {
            model.addAttribute("sites", siteService.list(null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("sites", java.util.List.of());
        }
        
        try {
            model.addAttribute("depts", deptService.list(null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("depts", java.util.List.of());
        }
    }
}

