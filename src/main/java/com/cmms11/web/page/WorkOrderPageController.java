package com.cmms11.web.page;

import com.cmms11.code.CodeService;
import com.cmms11.domain.dept.DeptService;
import com.cmms11.domain.site.SiteService;
import com.cmms11.workorder.WorkOrderResponse;
import com.cmms11.workorder.WorkOrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 이름: WorkOrderPageController
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 작업지시 관리 페이지 컨트롤러 (HTML 반환)
 */
@Controller
public class WorkOrderPageController {

    private final WorkOrderService service;
    private final CodeService codeService;
    private final SiteService siteService;
    private final DeptService deptService;

    public WorkOrderPageController(
        WorkOrderService service,
        CodeService codeService,
        SiteService siteService,
        DeptService deptService
    ) {
        this.service = service;
        this.codeService = codeService;
        this.siteService = siteService;
        this.deptService = deptService;
    }

    /**
     * 작업지시 목록 페이지
     */
    @GetMapping("/workorder/list")
    public String list(
        @RequestParam(required = false) String orderId,
        @RequestParam(required = false) String plantId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String stage,
        @RequestParam(required = false) String plannedDateFrom,
        @RequestParam(required = false) String plannedDateTo,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Pageable pageable,
        Model model
    ) {
        Page<WorkOrderResponse> page = service.list(
            orderId, plantId, status, stage, plannedDateFrom, plannedDateTo, pageable
        );
        
        model.addAttribute("page", page);
        model.addAttribute("orderId", orderId);
        model.addAttribute("plantId", plantId);
        model.addAttribute("status", status);
        model.addAttribute("stage", stage);
        model.addAttribute("plannedDateFrom", plannedDateFrom);
        model.addAttribute("plannedDateTo", plannedDateTo);
        
        return _fragment ? "workorder/list :: content" : "workorder/list";
    }

    /**
     * 작업지시 상세 페이지
     */
    @GetMapping("/workorder/detail/{orderId}")
    public String detail(
        @PathVariable String orderId,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Model model
    ) {
        WorkOrderResponse workOrder = service.get(orderId);
        model.addAttribute("workOrder", workOrder);
        
        return _fragment ? "workorder/detail :: content" : "workorder/detail";
    }

    /**
     * 작업지시 등록/수정 폼 페이지
     */
    @GetMapping("/workorder/form")
    public String form(
        @RequestParam(required = false) String id,
        @RequestParam(required = false) String stage,
        @RequestParam(required = false) String refEntity,
        @RequestParam(required = false) String refId,
        @RequestParam(required = false) String refStage,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Model model
    ) {
        boolean isNew = (id == null || id.isEmpty());
        WorkOrderResponse workOrder;
        
        if (isNew) {
            // 신규 등록 또는 참조 복사
            if (refId != null && !refId.isEmpty()) {
                workOrder = service.get(refId);
            } else {
                workOrder = null;
            }
        } else {
            // 수정 모드
            workOrder = service.get(id);
        }
        
        model.addAttribute("workOrder", workOrder);
        model.addAttribute("isNew", isNew);
        model.addAttribute("stage", stage);
        model.addAttribute("refEntity", refEntity);
        model.addAttribute("refId", refId);
        model.addAttribute("refStage", refStage);
        
        // Select box용 참조 데이터 추가
        addReferenceData(model);
        
        return _fragment ? "workorder/form :: content" : "workorder/form";
    }

    /**
     * Select box용 참조 데이터 추가
     */
    private void addReferenceData(Model model) {
        try {
            model.addAttribute("jobTypes", codeService.listItems("JOBTP", null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("jobTypes", java.util.List.of());
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

