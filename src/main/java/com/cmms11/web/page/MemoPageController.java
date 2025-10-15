package com.cmms11.web.page;

import com.cmms11.memo.MemoResponse;
import com.cmms11.memo.MemoService;
import com.cmms11.code.CodeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 이름: MemoPageController
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 메모/게시글 관리 페이지 컨트롤러 (HTML 반환)
 */
@Controller
public class MemoPageController {

    private final MemoService service;
    private final CodeService codeService;

    public MemoPageController(MemoService service, CodeService codeService) {
        this.service = service;
        this.codeService = codeService;
    }

    @GetMapping("/memo/list")
    public String list(
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String createdBy,
        @RequestParam(required = false) String refEntity,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String stage,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Pageable pageable,
        Model model
    ) {
        Page<MemoResponse> page = service.list(
            title, createdBy, refEntity, status, stage, pageable
        );
        
        model.addAttribute("page", page);
        model.addAttribute("title", title);
        model.addAttribute("createdBy", createdBy);
        model.addAttribute("refEntity", refEntity);
        model.addAttribute("status", status);
        model.addAttribute("stage", stage);
        
        // 모듈 코드 목록 추가 (참조 모듈용)
        try {
            model.addAttribute("moduleList", codeService.listItems("MODUL", null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("moduleList", java.util.List.of());
        }
        
        return _fragment ? "memo/list :: content" : "memo/list";
    }

    @GetMapping("/memo/detail/{memoId}")
    public String detail(
        @PathVariable String memoId,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Model model
    ) {
        MemoResponse memo = service.get(memoId);
        model.addAttribute("memo", memo);
        
        return _fragment ? "memo/detail :: content" : "memo/detail";
    }

    @GetMapping("/memo/form")
    public String form(
        @RequestParam(required = false) String id,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Model model
    ) {
        boolean isNew = (id == null || id.isEmpty());
        MemoResponse memo = isNew ? createEmptyMemo() : service.get(id);
        
        model.addAttribute("memo", memo);
        model.addAttribute("isNew", isNew);
        
        // 참조 모듈 목록 추가
        try {
            model.addAttribute("refModules", codeService.listItems("MODUL", null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("refModules", java.util.List.of());
        }
        
        return _fragment ? "memo/form :: content" : "memo/form";
    }

    /**
     * 빈 Memo 객체 생성 (신규 등록용)
     */
    private MemoResponse createEmptyMemo() {
        return new MemoResponse(
            null, null, null, null, null, null,
            null, null, null, null
        );
    }
}

