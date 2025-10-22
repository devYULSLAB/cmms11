package com.cmms11.web;

import com.cmms11.domain.company.CompanyRequest;
import com.cmms11.domain.company.CompanyResponse;
import com.cmms11.domain.company.CompanyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * 이름: CompanyController
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 회사 기준정보 웹 화면 및 API 엔드포인트를 제공하는 컨트롤러.
 */
@Controller
public class CompanyController {

    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    // 웹 컨트롤러 화면 제공
    @GetMapping("/domain/company/list")
    public String listForm(@RequestParam(name = "q", required = false) String q, Pageable pageable, Model model) {
        Page<CompanyResponse> page = service.list(q, pageable);
        model.addAttribute("page", page);
        model.addAttribute("keyword", q);
        return "domain/company/list";
    }

    @GetMapping("/domain/company/form")
    public String newForm(Model model) {
        model.addAttribute("company", new CompanyResponse(
            null, // companyId
            null, // name
            null, // bizNo
            null, // email
            null, // phone
            null, // note
            null, // deleteMark
            null, // createdAt
            null, // createdBy
            null, // updatedAt
            null  // updatedBy
        ));
        model.addAttribute("isNew", true);
        return "domain/company/form";
    }

    @GetMapping("/domain/company/edit/{companyId}")
    public String editForm(@PathVariable String companyId, Model model) {
        CompanyResponse company = service.get(companyId);
        model.addAttribute("company", company);
        model.addAttribute("isNew", false);
        return "domain/company/form";
    }

    @PostMapping("/domain/company/save")
    public String saveForm(@ModelAttribute CompanyRequest request, @RequestParam(required = false) String isNew) {
        if ("true".equals(isNew)) {
            service.create(request);
        } else {
            service.update(request.companyId(), request);
        }
        return "redirect:/layout/defaultLayout.html?content=/domain/company/list";
    }

    @PostMapping("/domain/company/delete/{companyId}")
    public String deleteForm(@PathVariable String companyId) {
        service.delete(companyId);
        return "redirect:/layout/defaultLayout.html?content=/domain/company/list";
    }

    // API 엔드포인트 제공
    // @ResponseBody
    // @GetMapping("/api/domain/companies")
    // public Page<CompanyResponse> list(@RequestParam(name = "q", required = false) String q, Pageable pageable) {
    //     return service.list(q, pageable);
    // }

    // @ResponseBody
    // @GetMapping("/api/domain/companies/{companyId}")
    // public ResponseEntity<CompanyResponse> get(@PathVariable String companyId) {
    //     return ResponseEntity.ok(service.get(companyId));
    // }

    // @ResponseBody
    // @PostMapping("/api/domain/companies")
    // public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CompanyRequest request) {
    //     CompanyResponse response = service.create(request);
    //     return ResponseEntity.status(HttpStatus.CREATED).body(response);
    // }

    // @ResponseBody
    // @PutMapping("/api/domain/companies/{companyId}")
    // public ResponseEntity<CompanyResponse> update(
    //     @PathVariable String companyId,
    //     @Valid @RequestBody CompanyRequest request
    // ) {
    //     CompanyResponse response = service.update(companyId, request);
    //     return ResponseEntity.ok(response);
    // }

    // @ResponseBody
    // @DeleteMapping("/api/domain/companies/{companyId}")
    // public ResponseEntity<Void> delete(@PathVariable String companyId) {
    //     service.delete(companyId);
    //     return ResponseEntity.noContent().build();
    // }
}
