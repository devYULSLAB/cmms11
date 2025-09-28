package com.cmms11.web;

import com.cmms11.memo.MemoRequest;
import com.cmms11.memo.MemoResponse;
import com.cmms11.memo.MemoService;
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
 * 이름: MemoController
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 메모 웹 화면 및 API 엔드포인트를 제공하는 컨트롤러.
 */
@Controller
public class MemoController {

    private final MemoService service;
    private final CodeService codeService;

    public MemoController(MemoService service, CodeService codeService) {
        this.service = service;
        this.codeService = codeService;
    }

    // 웹 컨트롤러 화면 제공
    @GetMapping("/memo/list")
    public String listForm(@RequestParam(name = "title", required = false) String title,
                          @RequestParam(name = "createdBy", required = false) String createdBy,
                          @RequestParam(name = "refEntity", required = false) String refEntity,
                          Pageable pageable, Model model) {
        Page<MemoResponse> page = service.list(title, createdBy, refEntity, pageable);
        model.addAttribute("page", page);
        model.addAttribute("title", title);
        model.addAttribute("createdBy", createdBy);
        model.addAttribute("refEntity", refEntity);
        return "memo/list";
    }

    @GetMapping("/memo/form")
    public String newForm(Model model) {
        model.addAttribute("memo", emptyMemo());
        model.addAttribute("isNew", true);
        addReferenceData(model);
        return "memo/form";
    }

    @GetMapping("/memo/detail/{memoId}")
    public String detailForm(@PathVariable String memoId, Model model) {
        MemoResponse memo = service.get(memoId);
        model.addAttribute("memo", memo);
        return "memo/detail";
    }

    @GetMapping("/memo/edit/{memoId}")
    public String editForm(@PathVariable String memoId, Model model) {
        MemoResponse memo = service.get(memoId);
        model.addAttribute("memo", memo);
        model.addAttribute("isNew", false);
        addReferenceData(model);
        return "memo/form";
    }

    @PostMapping("/memo/save")
    public String saveForm(@ModelAttribute MemoRequest request, @RequestParam(required = false) String isNew) {
        if ("true".equals(isNew)) {
            service.create(request);
        } else {
            service.update(request.memoId(), request);
        }
        return "redirect:/memo/list";
    }

    @PostMapping("/memo/delete/{memoId}")
    public String deleteForm(@PathVariable String memoId) {
        service.delete(memoId);
        return "redirect:/memo/list";
    }

    // API 엔드포인트 제공
    @ResponseBody
    @GetMapping("/api/memos")
    public Page<MemoResponse> list(@RequestParam(name = "q", required = false) String q, 
                                  @RequestParam(name = "title", required = false) String title,
                                  @RequestParam(name = "createdBy", required = false) String createdBy,
                                  @RequestParam(name = "refEntity", required = false) String refEntity,
                                  Pageable pageable) {
        // 다중 필터가 있으면 다중 필터 사용, 없으면 기존 keyword 검색 사용
        if (title != null || createdBy != null || refEntity != null) {
            return service.list(title, createdBy, refEntity, pageable);
        } else {
            return service.list(q, pageable);
        }
    }

    @ResponseBody
    @GetMapping("/api/memos/{memoId}")
    public ResponseEntity<MemoResponse> get(@PathVariable String memoId) {
        return ResponseEntity.ok(service.get(memoId));
    }

    @ResponseBody
    @PostMapping("/api/memos")
    public ResponseEntity<MemoResponse> create(@Valid @RequestBody MemoRequest request) {
        MemoResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ResponseBody
    @PutMapping("/api/memos/{memoId}")
    public ResponseEntity<MemoResponse> update(
        @PathVariable String memoId,
        @Valid @RequestBody MemoRequest request
    ) {
        MemoResponse response = service.update(memoId, request);
        return ResponseEntity.ok(response);
    }

    @ResponseBody
    @DeleteMapping("/api/memos/{memoId}")
    public ResponseEntity<Void> delete(@PathVariable String memoId) {
        service.delete(memoId);
        return ResponseEntity.noContent().build();
    }

    private MemoResponse emptyMemo() {
        return new MemoResponse(
            null, // memoId
            null, // title
            null, // content
            null, // refEntity
            null, // refId
            null, // fileGroupId
            null, // createdAt
            null, // createdBy
            null, // updatedAt
            null  // updatedBy
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
    }
}
