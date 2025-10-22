package com.cmms11.web;

import com.cmms11.domain.func.FuncRequest;
import com.cmms11.domain.func.FuncResponse;
import com.cmms11.domain.func.FuncService;
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
 * 이름: FuncController
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 기능위치 기준정보 웹 화면 및 API 엔드포인트를 제공하는 컨트롤러.
 */
@Controller
public class FuncController {

    private final FuncService service;

    public FuncController(FuncService service) {
        this.service = service;
    }

    // 웹 컨트롤러 화면 제공
    @GetMapping("/domain/func/list")
    public String listForm(@RequestParam(name = "q", required = false) String q, Pageable pageable, Model model) {
        Page<FuncResponse> page = service.list(q, pageable);
        model.addAttribute("page", page);
        model.addAttribute("keyword", q);
        return "domain/func/list";
    }

    @GetMapping("/domain/func/form")
    public String newForm(Model model) {
        model.addAttribute("func", emptyFunc());
        model.addAttribute("isNew", true);
        return "domain/func/form";
    }

    @GetMapping("/domain/func/edit/{funcId}")
    public String editForm(@PathVariable String funcId, Model model) {
        FuncResponse func = service.get(funcId);
        model.addAttribute("func", func);
        model.addAttribute("isNew", false);
        return "domain/func/form";
    }

    @PostMapping("/domain/func/save")
    public String save(@ModelAttribute FuncRequest request, @RequestParam(required = false) String isNew) {
        if ("true".equals(isNew)) {
            service.create(request);
        } else {
            service.update(request.funcId(), request);
        }
        return "redirect:/layout/defaultLayout.html?content=/domain/func/list";
    }

    @PostMapping("/domain/func/delete/{funcId}")
    public String deleteForm(@PathVariable String funcId) {
        service.delete(funcId);
        return "redirect:/layout/defaultLayout.html?content=/domain/func/list";
    }

    // API 엔드포인트 제공 (조회만 - picker용)
    @ResponseBody
    @GetMapping("/api/domain/funcs")
    public Page<FuncResponse> list(@RequestParam(name = "q", required = false) String q, Pageable pageable) {
        return service.list(q, pageable);
    }

    // @ResponseBody
    // @GetMapping("/api/domain/funcs/{funcId}")
    // public ResponseEntity<FuncResponse> get(@PathVariable String funcId) {
    //     return ResponseEntity.ok(service.get(funcId));
    // }

    // @ResponseBody
    // @PostMapping("/api/domain/funcs")
    // public ResponseEntity<FuncResponse> create(@Valid @RequestBody FuncRequest request) {
    //     FuncResponse response = service.create(request);
    //     return ResponseEntity.status(HttpStatus.CREATED).body(response);
    // }

    // @ResponseBody
    // @PutMapping("/api/domain/funcs/{funcId}")
    // public ResponseEntity<FuncResponse> update(
    //     @PathVariable String funcId,
    //     @Valid @RequestBody FuncRequest request
    // ) {
    //     FuncResponse response = service.update(funcId, request);
    //     return ResponseEntity.ok(response);
    // }

    // @ResponseBody
    // @DeleteMapping("/api/domain/funcs/{funcId}")
    // public ResponseEntity<Void> delete(@PathVariable String funcId) {
    //     service.delete(funcId);
    //     return ResponseEntity.noContent().build();
    // }

    private FuncResponse emptyFunc() {
        return new FuncResponse(
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
