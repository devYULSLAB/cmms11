package com.cmms11.web;

import com.cmms11.domain.dept.DeptRequest;
import com.cmms11.domain.dept.DeptResponse;
import com.cmms11.domain.dept.DeptService;
import jakarta.validation.Valid;
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
 * 이름: DeptController
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 부서 기준정보 웹 화면 및 API 엔드포인트를 제공하는 컨트롤러.
 */
@Controller
public class DeptController {

    private final DeptService service;

    public DeptController(DeptService service) {
        this.service = service;
    }

    // 웹 컨트롤러 화면 제공
    @GetMapping("/domain/dept/list")
    public String listForm(@RequestParam(name = "q", required = false) String q, Pageable pageable, Model model) {
        Page<DeptResponse> page = service.list(q, pageable);
        model.addAttribute("page", page);
        model.addAttribute("keyword", q);
        return "domain/dept/list";
    }

    @GetMapping("/domain/dept/form")
    public String newForm(Model model) {
        model.addAttribute("dept", emptyDept());
        model.addAttribute("isNew", true);
        return "domain/dept/form";
    }

    @GetMapping("/domain/dept/edit/{deptId}")
    public String editForm(@PathVariable String deptId, Model model) {
        DeptResponse dept = service.get(deptId);
        model.addAttribute("dept", dept);
        model.addAttribute("isNew", false);
        return "domain/dept/form";
    }

    @PostMapping("/domain/dept/save")
    public String save(@ModelAttribute DeptRequest request, @RequestParam(required = false) String isNew) {
        if ("true".equals(isNew)) {
            service.create(request);
        } else {
            service.update(request.deptId(), request);
        }
        return "redirect:/domain/dept/list";
    }

    @PostMapping("/domain/dept/delete/{deptId}")
    public String deleteForm(@PathVariable String deptId) {
        service.delete(deptId);
        return "redirect:/domain/dept/list";
    }

    // API 엔드포인트 제공 (조회만 - picker용)
    @ResponseBody
    @GetMapping("/api/domain/depts")
    public Page<DeptResponse> list(@RequestParam(name = "q", required = false) String q, Pageable pageable) {
        return service.list(q, pageable);
    }

    // @ResponseBody
    // @GetMapping("/api/domain/depts/{deptId}")
    // public ResponseEntity<DeptResponse> get(@PathVariable String deptId) {
    //     return ResponseEntity.ok(service.get(deptId));
    // }

    // @ResponseBody
    // @PostMapping("/api/domain/depts")
    // public ResponseEntity<DeptResponse> create(@Valid @RequestBody DeptRequest request) {
    //     DeptResponse response = service.create(request);
    //     return ResponseEntity.status(HttpStatus.CREATED).body(response);
    // }

    // @ResponseBody
    // @PutMapping("/api/domain/depts/{deptId}")
    // public ResponseEntity<DeptResponse> update(
    //     @PathVariable String deptId,
    //     @Valid @RequestBody DeptRequest request
    // ) {
    //     DeptResponse response = service.update(deptId, request);
    //     return ResponseEntity.ok(response);
    // }

    // @ResponseBody
    // @DeleteMapping("/api/domain/depts/{deptId}")
    // public ResponseEntity<Void> delete(@PathVariable String deptId) {
    //     service.delete(deptId);
    //     return ResponseEntity.noContent().build();
    // }

    private DeptResponse emptyDept() {
        return new DeptResponse(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "N",
            null,
            null,
            null,
            null
        );
    }
}
