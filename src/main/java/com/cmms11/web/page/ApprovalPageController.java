package com.cmms11.web.page;

import com.cmms11.approval.ApprovalResponse;
import com.cmms11.approval.ApprovalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 이름: ApprovalPageController
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 결재 관리 페이지 컨트롤러 (HTML 반환)
 */
@Controller
public class ApprovalPageController {

    private final ApprovalService service;

    public ApprovalPageController(ApprovalService service) {
        this.service = service;
    }

    @GetMapping("/approval/list")
    public String list(
        @RequestParam(required = false) String status,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Pageable pageable,
        Model model
    ) {
        Page<ApprovalResponse> page = service.list(status, pageable);
        
        model.addAttribute("page", page);
        model.addAttribute("status", status);
        
        return _fragment ? "approval/list :: content" : "approval/list";
    }

    @GetMapping("/approval/detail/{approvalId}")
    public String detail(
        @PathVariable String approvalId,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Model model
    ) {
        ApprovalResponse approval = service.get(approvalId);
        model.addAttribute("approval", approval);
        
        return _fragment ? "approval/detail :: content" : "approval/detail";
    }

    @GetMapping("/approval/form")
    public String form(
        @RequestParam(required = false) String id,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Model model
    ) {
        boolean isNew = (id == null || id.isEmpty());
        ApprovalResponse approval = isNew ? null : service.get(id);
        
        model.addAttribute("approval", approval);
        model.addAttribute("isNew", isNew);
        
        return _fragment ? "approval/form :: content" : "approval/form";
    }
}

