package com.cmms11.web;

import com.cmms11.domain.site.SiteRequest;
import com.cmms11.domain.site.SiteResponse;
import com.cmms11.domain.site.SiteService;
import com.cmms11.domain.company.CompanyService;
import com.cmms11.domain.company.CompanyResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
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
 * 이름: SiteController
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 사업장 기준정보 웹 화면 및 API 엔드포인트를 제공하는 컨트롤러.
 */
@Controller
public class SiteController {

    private final SiteService service;
    private final CompanyService companyService;

    public SiteController(SiteService service, CompanyService companyService) {
        this.service = service;
        this.companyService = companyService;
    }

    // 웹 컨트롤러 화면 제공
    @GetMapping("/domain/site/list")
    public String listForm(@RequestParam(name = "q", required = false) String q, Pageable pageable, Model model) {
        Page<SiteResponse> page = service.list(q, pageable);
        model.addAttribute("page", page);
        model.addAttribute("keyword", q);
        return "domain/site/list";
    }

    @GetMapping("/domain/site/form")
    public String newForm(Model model) {
        model.addAttribute("site", emptySite());
        model.addAttribute("isNew", true);
        model.addAttribute("companies", getCompanyList());
        return "domain/site/form";
    }

    @GetMapping("/domain/site/edit/{siteId}")
    public String editForm(@PathVariable String siteId, Model model) {
        SiteResponse site = service.get(siteId);
        model.addAttribute("site", site);
        model.addAttribute("isNew", false);
        model.addAttribute("companies", getCompanyList());
        return "domain/site/form";
    }

    @PostMapping("/domain/site/save")
    public String save(@ModelAttribute SiteRequest request, @RequestParam(required = false) String isNew) {
        if ("true".equals(isNew)) {
            service.create(request);
        } else {
            service.update(request.siteId(), request);
        }
        return "redirect:/layout/defaultLayout.html?content=/domain/site/list";
    }

    @PostMapping("/domain/site/delete/{siteId}")
    public String deleteForm(@PathVariable String siteId) {
        service.delete(siteId);
        return "redirect:/layout/defaultLayout.html?content=/domain/site/list";
    }

    // API 엔드포인트 제공 (불필요 - 서버사이드에서만 참조)
    // @ResponseBody
    // @GetMapping("/api/domain/sites")
    // public Page<SiteResponse> list(@RequestParam(name = "q", required = false) String q, Pageable pageable) {
    //     return service.list(q, pageable);
    // }

    // API 엔드포인트 제공
    // @ResponseBody
    // @GetMapping("/api/domain/sites/{siteId}")
    // public ResponseEntity<SiteResponse> get(@PathVariable String siteId) {
    //     return ResponseEntity.ok(service.get(siteId));
    // }

    // @ResponseBody
    // @PostMapping("/api/domain/sites")
    // public ResponseEntity<SiteResponse> create(@Valid @RequestBody SiteRequest request) {
    //     SiteResponse response = service.create(request);
    //     return ResponseEntity.status(HttpStatus.CREATED).body(response);
    // }

    // @ResponseBody
    // @PutMapping("/api/domain/sites/{siteId}")
    // public ResponseEntity<SiteResponse> update(
    //     @PathVariable String siteId,
    //     @Valid @RequestBody SiteRequest request
    // ) {
    //     SiteResponse response = service.update(siteId, request);
    //     return ResponseEntity.ok(response);
    // }

    // @ResponseBody
    // @DeleteMapping("/api/domain/sites/{siteId}")
    // public ResponseEntity<Void> delete(@PathVariable String siteId) {
    //     service.delete(siteId);
    //     return ResponseEntity.noContent().build();
    // }

    private SiteResponse emptySite() {
        return new SiteResponse(
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

    private List<CompanyResponse> getCompanyList() {
        // 실제 회사 목록을 CompanyService에서 가져옴
        Page<CompanyResponse> companies = companyService.list(null, PageRequest.of(0, 1000));
        return companies.getContent();
    }
}
