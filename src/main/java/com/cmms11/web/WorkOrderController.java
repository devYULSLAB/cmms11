package com.cmms11.web;

import com.cmms11.workorder.WorkOrderRequest;
import com.cmms11.workorder.WorkOrderResponse;
import com.cmms11.workorder.WorkOrderService;
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
 * 이름: WorkOrderController
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 작업지시 웹 화면 및 API 엔드포인트를 제공하는 컨트롤러.
 */
@Controller
public class WorkOrderController {

    private final WorkOrderService service;
    private final CodeService codeService;
    private final SiteService siteService;
    private final DeptService deptService;

    public WorkOrderController(WorkOrderService service, CodeService codeService, SiteService siteService, DeptService deptService) {
        this.service = service;
        this.codeService = codeService;
        this.siteService = siteService;
        this.deptService = deptService;
    }

    // 웹 컨트롤러 화면 제공
    @GetMapping("/workorder/list")
    public String listForm(@RequestParam(name = "orderId", required = false) String orderId,
                          @RequestParam(name = "plantId", required = false) String plantId,
                          @RequestParam(name = "status", required = false) String status,
                          @RequestParam(name = "plannedDateFrom", required = false) String plannedDateFrom,
                          @RequestParam(name = "plannedDateTo", required = false) String plannedDateTo,
                          Pageable pageable, Model model) {
        Page<WorkOrderResponse> page = service.list(orderId, plantId, status, plannedDateFrom, plannedDateTo, pageable);
        model.addAttribute("page", page);
        model.addAttribute("orderId", orderId);
        model.addAttribute("plantId", plantId);
        model.addAttribute("status", status);
        model.addAttribute("plannedDateFrom", plannedDateFrom);
        model.addAttribute("plannedDateTo", plannedDateTo);
        return "workorder/list";
    }

    @GetMapping("/workorder/form")
    public String newForm(Model model) {
        model.addAttribute("workOrder", emptyWorkOrder());
        model.addAttribute("isNew", true);
        addReferenceData(model);
        return "workorder/form";
    }

    @GetMapping("/workorder/edit/{workOrderId}")
    public String editForm(@PathVariable String workOrderId, Model model) {
        WorkOrderResponse workOrder = service.get(workOrderId);
        model.addAttribute("workOrder", workOrder);
        model.addAttribute("isNew", false);
        addReferenceData(model);
        return "workorder/form";
    }

    @GetMapping("/workorder/detail/{workOrderId}")
    public String detailForm(@PathVariable String workOrderId, Model model) {
        WorkOrderResponse workOrder = service.get(workOrderId);
        model.addAttribute("workorder", workOrder);
        return "workorder/detail";
    }

    @PostMapping("/workorder/save")
    public String saveForm(@ModelAttribute WorkOrderRequest request, @RequestParam(required = false) String isNew) {
        if ("true".equals(isNew)) {
            service.create(request);
        } else {
            service.update(request.orderId(), request);
        }
        return "redirect:/workorder/list";
    }

    @PostMapping("/workorder/delete/{workOrderId}")
    public String deleteForm(@PathVariable String workOrderId) {
        service.delete(workOrderId);
        return "redirect:/workorder/list";
    }

    // API 엔드포인트 제공
    @ResponseBody
    @GetMapping("/api/workorders")
    public Page<WorkOrderResponse> list(@RequestParam(name = "orderId", required = false) String orderId,
                                       @RequestParam(name = "plantId", required = false) String plantId,
                                       @RequestParam(name = "status", required = false) String status,
                                       @RequestParam(name = "plannedDateFrom", required = false) String plannedDateFrom,
                                       @RequestParam(name = "plannedDateTo", required = false) String plannedDateTo,
                                       Pageable pageable) {
        return service.list(orderId, plantId, status, plannedDateFrom, plannedDateTo, pageable);
    }

    @ResponseBody
    @GetMapping("/api/workorders/{workOrderId}")
    public ResponseEntity<WorkOrderResponse> get(@PathVariable String workOrderId) {
        return ResponseEntity.ok(service.get(workOrderId));
    }

    @ResponseBody
    @PostMapping("/api/workorders")
    public ResponseEntity<WorkOrderResponse> create(@Valid @RequestBody WorkOrderRequest request) {
        WorkOrderResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ResponseBody
    @PutMapping("/api/workorders/{workOrderId}")
    public ResponseEntity<WorkOrderResponse> update(
        @PathVariable String workOrderId,
        @Valid @RequestBody WorkOrderRequest request
    ) {
        WorkOrderResponse response = service.update(workOrderId, request);
        return ResponseEntity.ok(response);
    }

    @ResponseBody
    @DeleteMapping("/api/workorders/{workOrderId}")
    public ResponseEntity<Void> delete(@PathVariable String workOrderId) {
        service.delete(workOrderId);
        return ResponseEntity.noContent().build();
    }

    private WorkOrderResponse emptyWorkOrder() {
        return new WorkOrderResponse(
            null, // workOrderId
            null, // name
            null, // plantId
            null, // jobId
            null, // siteId
            null, // deptId
            null, // memberId
            null, // plannedDate
            null, // plannedCost
            null, // plannedLabor
            null, // actualDate
            null, // actualCost
            null, // actualLabor
            "PLAN", // status - 기본값 설정
            null, // fileGroupId
            null, // note
            null, // createdAt
            null, // createdBy
            null, // updatedAt
            null  // updatedBy
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
