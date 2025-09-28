package com.cmms11.web;

import com.cmms11.approval.ApprovalRequest;
import com.cmms11.approval.ApprovalResponse;
import com.cmms11.approval.ApprovalService;
import com.cmms11.code.CodeService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이름: ApprovalController
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 전자결재 웹 화면 및 API 엔드포인트를 제공하는 컨트롤러.
 */
@Controller
public class ApprovalController {

    private final ApprovalService service;
    private final CodeService codeService;

    public ApprovalController(ApprovalService service, CodeService codeService) {
        this.service = service;
        this.codeService = codeService;
    }

    // 웹 컨트롤러 화면 제공
    @GetMapping("/approval/list")
    public String listForm(@RequestParam(name = "title", required = false) String title,
                          @RequestParam(name = "createdBy", required = false) String createdBy,
                          @RequestParam(name = "status", required = false) String status,
                          Pageable pageable, Model model) {
        Page<ApprovalResponse> page = service.list(title, createdBy, status, pageable);
        model.addAttribute("page", page);
        model.addAttribute("title", title);
        model.addAttribute("createdBy", createdBy);
        model.addAttribute("status", status);
        return "approval/list";
    }

    @GetMapping("/approval/form")
    public String newForm(Model model) {
        model.addAttribute("approval", emptyApproval());
        model.addAttribute("isNew", true);
        addReferenceData(model);
        return "approval/form";
    }

    @GetMapping("/approval/detail/{approvalId}")
    public String detailForm(@PathVariable String approvalId, Model model) {
        ApprovalResponse approval = service.get(approvalId);
        model.addAttribute("approval", approval);
        return "approval/detail";
    }

    @GetMapping("/approval/edit/{approvalId}")
    public String editForm(@PathVariable String approvalId, Model model) {
        ApprovalResponse approval = service.get(approvalId);
        model.addAttribute("approval", approval);
        model.addAttribute("isNew", false);
        addReferenceData(model);
        return "approval/form";
    }

    @PostMapping("/approval/save")
    public String saveForm(@ModelAttribute ApprovalRequest request, @RequestParam(required = false) String isNew) {
        if ("true".equals(isNew)) {
            service.create(request);
        } else {
            service.update(request.approvalId(), request);
        }
        return "redirect:/approval/list";
    }

    @PostMapping("/approval/delete/{approvalId}")
    public String deleteForm(@PathVariable String approvalId) {
        service.delete(approvalId);
        return "redirect:/approval/list";
    }

    // API 엔드포인트 제공
    @ResponseBody
    @GetMapping("/api/approvals")
    public Page<ApprovalResponse> list(@RequestParam(name = "q", required = false) String q, 
                                      @RequestParam(name = "title", required = false) String title,
                                      @RequestParam(name = "createdBy", required = false) String createdBy,
                                      @RequestParam(name = "status", required = false) String status,
                                      Pageable pageable) {
        // 다중 필터가 있으면 다중 필터 사용, 없으면 기존 keyword 검색 사용
        if (title != null || createdBy != null || status != null) {
            return service.list(title, createdBy, status, pageable);
        } else {
            return service.list(q, pageable);
        }
    }

    @ResponseBody
    @GetMapping("/api/approvals/{approvalId}")
    public ResponseEntity<ApprovalResponse> get(@PathVariable String approvalId) {
        return ResponseEntity.ok(service.get(approvalId));
    }

    @ResponseBody
    @PostMapping("/api/approvals")
    public ResponseEntity<ApprovalResponse> create(@Valid @RequestBody ApprovalRequest request) {
        ApprovalResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ResponseBody
    @PutMapping("/api/approvals/{approvalId}")
    public ResponseEntity<ApprovalResponse> update(
        @PathVariable String approvalId,
        @Valid @RequestBody ApprovalRequest request
    ) {
        ApprovalResponse response = service.update(approvalId, request);
        return ResponseEntity.ok(response);
    }

    @ResponseBody
    @DeleteMapping("/api/approvals/{approvalId}")
    public ResponseEntity<Void> delete(@PathVariable String approvalId) {
        service.delete(approvalId);
        return ResponseEntity.noContent().build();
    }

    private ApprovalResponse emptyApproval() {
        return new ApprovalResponse(
            null, // approvalId
            null, // title
            null, // status
            null, // refEntity
            null, // refId
            null, // content
            null, // fileGroupId
            null, // createdAt
            null, // createdBy
            null, // updatedAt
            null, // updatedBy
            null  // steps
        );
    }

    private void addReferenceData(Model model) {
        // 참조 모듈 목록 (MODUL 코드 타입)
        try {
            model.addAttribute("refModules", codeService.listItems("MODUL", null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            // 기본 참조 모듈 목록 제공
            model.addAttribute("refModules", List.of(
                Map.of("code", "PLANT", "name", "설비"),
                Map.of("code", "INV", "name", "자재/재고"),
                Map.of("code", "INSP", "name", "점검"),
                Map.of("code", "WO", "name", "작업지시"),
                Map.of("code", "WP", "name", "작업허가")
            ));
        }

        // 결재 상태 목록
        model.addAttribute("statusList", List.of(
            Map.of("code", "DRAFT", "name", "임시저장"),
            Map.of("code", "SUBMIT", "name", "상신"),
            Map.of("code", "APPROVAL", "name", "결재중"),
            Map.of("code", "COMPLETE", "name", "완료"),
            Map.of("code", "REJECT", "name", "반려")
        ));
    }
}
