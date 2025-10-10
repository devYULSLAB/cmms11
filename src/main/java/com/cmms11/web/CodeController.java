package com.cmms11.web;

import com.cmms11.code.CodeItemRequest;
import com.cmms11.code.CodeItemResponse;
import com.cmms11.code.CodeService;
import com.cmms11.code.CodeTypeRequest;
import com.cmms11.code.CodeTypeResponse;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
 * 이름: CodeController
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 공통 코드 타입 및 항목을 관리하는 웹/REST 컨트롤러.
 */
@Controller
public class CodeController {

    private final CodeService service;

    public CodeController(CodeService service) {
        this.service = service;
    }

    // 웹 컨트롤러 화면 제공
    @GetMapping("/code/list")
    public String listForm(Pageable pageable, Model model) {
        Page<CodeTypeResponse> page = service.listTypes(null, pageable);
        model.addAttribute("page", page);
        return "code/list";
    }

    @GetMapping("/code/form")
    public String newForm(Model model) {
        CodeForm form = new CodeForm();
        model.addAttribute("type", form);
        model.addAttribute("items", new ArrayList<>());
        model.addAttribute("isNew", true);
        return "code/form";
    }

    @GetMapping("/code/edit/{codeType}")
    public String editForm(@PathVariable String codeType, Model model) {
        try {
            CodeTypeResponse type = service.getType(codeType);
            List<CodeItemResponse> items = service.listItems(codeType, null, Pageable.unpaged()).getContent();
            CodeForm form = new CodeForm();
            form.setCodeType(type.codeType());
            form.setName(type.name());
            form.setNote(type.note());
            form.setItems(items.stream().map(CodeItemForm::from).collect(Collectors.toCollection(ArrayList::new)));
            model.addAttribute("type", form);
            model.addAttribute("items", form.getItems());
            model.addAttribute("isNew", false);
            return "code/form";
        } catch (Exception e) {
            // 코드 타입이 존재하지 않는 경우 목록으로 리다이렉트
            return "redirect:/code/list?error=notfound";
        }
    }

    @PostMapping("/code/save")
    public String saveForm(@ModelAttribute CodeForm form, @RequestParam(required = false) String isNew) {
        CodeTypeRequest typeRequest = new CodeTypeRequest(form.getCodeType(), form.getName(), form.getNote());
        boolean create = "true".equals(isNew);
        if (create) {
            service.createType(typeRequest);
        } else {
            service.updateType(form.getCodeType(), typeRequest);
        }

        List<CodeItemForm> submittedItems = Optional.ofNullable(form.getItems()).orElseGet(ArrayList::new);
        if (create) {
            for (CodeItemForm item : submittedItems) {
                if (item.isBlank()) {
                    continue;
                }
                CodeItemRequest request = new CodeItemRequest(form.getCodeType(), item.getCode(), item.getName(), item.getNote());
                service.createItem(request);
            }
        } else {
            Map<String, CodeItemResponse> existing = service
                .listItems(form.getCodeType(), null, Pageable.unpaged())
                .getContent()
                .stream()
                .collect(Collectors.toMap(CodeItemResponse::code, it -> it, (a, b) -> a, HashMap::new));

            for (CodeItemForm item : submittedItems) {
                if (item.isBlank()) {
                    continue;
                }
                CodeItemRequest request = new CodeItemRequest(form.getCodeType(), item.getCode(), item.getName(), item.getNote());
                if (existing.containsKey(item.getCode())) {
                    service.updateItem(form.getCodeType(), item.getCode(), request);
                    existing.remove(item.getCode());
                } else {
                    service.createItem(request);
                }
            }

            for (String code : existing.keySet()) {
                service.deleteItem(form.getCodeType(), code);
            }
        }

        return "redirect:/code/list";
    }

    @PostMapping("/code/delete/{codeType}")
    public String deleteForm(@PathVariable String codeType) {
        List<CodeItemResponse> items = service.listItems(codeType, null, Pageable.unpaged()).getContent();
        for (CodeItemResponse item : items) {
            service.deleteItem(codeType, item.code());
        }
        service.deleteType(codeType);
        return "redirect:/code/list";
    }

    // API 엔드포인트 제공
    @ResponseBody
    @GetMapping("/api/codes/types")
    public Page<CodeTypeResponse> listTypes(@RequestParam(name = "q", required = false) String q, Pageable pageable) {
        return service.listTypes(q, pageable);
    }

    @ResponseBody
    @GetMapping("/api/codes/types/{codeType}")
    public ResponseEntity<CodeTypeResponse> getType(@PathVariable String codeType) {
        return ResponseEntity.ok(service.getType(codeType));
    }

    @ResponseBody
    @PostMapping("/api/codes/types")
    public ResponseEntity<CodeTypeResponse> createType(@Valid @RequestBody CodeTypeRequest request) {
        CodeTypeResponse response = service.createType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ResponseBody
    @PutMapping("/api/codes/types/{codeType}")
    public ResponseEntity<CodeTypeResponse> updateType(
        @PathVariable String codeType,
        @Valid @RequestBody CodeTypeRequest request
    ) {
        CodeTypeResponse response = service.updateType(codeType, request);
        return ResponseEntity.ok(response);
    }

    @ResponseBody
    @DeleteMapping("/api/codes/types/{codeType}")
    public ResponseEntity<Void> deleteType(@PathVariable String codeType) {
        service.deleteType(codeType);
        return ResponseEntity.noContent().build();
    }

    @ResponseBody
    @GetMapping("/api/codes/types/{codeType}/items")
    public Page<CodeItemResponse> listItems(
        @PathVariable String codeType,
        @RequestParam(name = "q", required = false) String q,
        Pageable pageable
    ) {
        return service.listItems(codeType, q, pageable);
    }

    @ResponseBody
    @GetMapping("/api/codes/types/{codeType}/items/{code}")
    public ResponseEntity<CodeItemResponse> getItem(@PathVariable String codeType, @PathVariable String code) {
        return ResponseEntity.ok(service.getItem(codeType, code));
    }

    @ResponseBody
    @PostMapping("/api/codes/types/{codeType}/items")
    public ResponseEntity<CodeItemResponse> createItem(
        @PathVariable String codeType,
        @Valid @RequestBody CodeItemRequest request
    ) {
        CodeItemRequest payload = new CodeItemRequest(codeType, request.code(), request.name(), request.note());
        CodeItemResponse response = service.createItem(payload);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ResponseBody
    @PutMapping("/api/codes/types/{codeType}/items/{code}")
    public ResponseEntity<CodeItemResponse> updateItem(
        @PathVariable String codeType,
        @PathVariable String code,
        @Valid @RequestBody CodeItemRequest request
    ) {
        CodeItemRequest payload = new CodeItemRequest(codeType, code, request.name(), request.note());
        CodeItemResponse response = service.updateItem(codeType, code, payload);
        return ResponseEntity.ok(response);
    }

    @ResponseBody
    @DeleteMapping("/api/codes/types/{codeType}/items/{code}")
    public ResponseEntity<Void> deleteItem(@PathVariable String codeType, @PathVariable String code) {
        service.deleteItem(codeType, code);
        return ResponseEntity.noContent().build();
    }

    public static class CodeForm {
        private String codeType;
        private String name;
        private String note;
        private List<CodeItemForm> items = new ArrayList<>();

        public String getCodeType() {
            return codeType;
        }

        public void setCodeType(String codeType) {
            this.codeType = codeType;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

        public List<CodeItemForm> getItems() {
            return items;
        }

        public void setItems(List<CodeItemForm> items) {
            this.items = items;
        }
    }

    public static class CodeItemForm {
        private String code;
        private String name;
        private String note;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

        private boolean isBlank() {
            return (code == null || code.isBlank()) || (name == null || name.isBlank());
        }

        private static CodeItemForm from(CodeItemResponse response) {
            CodeItemForm form = new CodeItemForm();
            form.setCode(response.code());
            form.setName(response.name());
            form.setNote(response.note());
            return form;
        }
    }
}

