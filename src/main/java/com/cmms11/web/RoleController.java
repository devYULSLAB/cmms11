package com.cmms11.web;

import com.cmms11.domain.role.RoleRequest;
import com.cmms11.domain.role.RoleResponse;
import com.cmms11.domain.role.RoleService;
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

@Controller
public class RoleController {

    private final RoleService service;

    public RoleController(RoleService service) {
        this.service = service;
    }

    @GetMapping("/domain/role/list")
    public String listForm(@RequestParam(name = "q", required = false) String q, Pageable pageable, Model model) {
        Page<RoleResponse> page = service.list(q, pageable);
        model.addAttribute("page", page);
        model.addAttribute("keyword", q);
        model.addAttribute("ready", Boolean.TRUE);
        return "domain/role/list";
    }

    @GetMapping("/domain/role/form")
    public String newForm(Model model) {
        model.addAttribute("role", new RoleResponse(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ));
        model.addAttribute("isNew", true);
        return "domain/role/form";
    }

    @GetMapping("/domain/role/edit/{roleId}")
    public String editForm(@PathVariable String roleId, Model model) {
        RoleResponse role = service.get(roleId);
        model.addAttribute("role", role);
        model.addAttribute("isNew", false);
        return "domain/role/form";
    }

    @PostMapping("/domain/role/save")
    public String saveForm(@ModelAttribute RoleRequest request, @RequestParam(required = false) String isNew) {
        if ("true".equals(isNew)) {
            service.create(request);
        } else {
            service.update(request.roleId(), request);
        }
        return "redirect:/domain/role/list";
    }

    @PostMapping("/domain/role/delete/{roleId}")
    public String deleteForm(@PathVariable String roleId) {
        service.delete(roleId);
        return "redirect:/domain/role/list";
    }

    // @ResponseBody
    // @GetMapping("/api/domain/roles")
    // public Page<RoleResponse> list(@RequestParam(name = "q", required = false) String q, Pageable pageable) {
    //     return service.list(q, pageable);
    // }

    // @ResponseBody
    // @GetMapping("/api/domain/roles/{roleId}")
    // public ResponseEntity<RoleResponse> get(@PathVariable("roleId") String roleId) {
    //     return ResponseEntity.ok(service.get(roleId));
    // }

    // @ResponseBody
    // @PostMapping("/api/domain/roles")
    // public ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleRequest request) {
    //     RoleResponse response = service.create(request);
    //     return ResponseEntity.status(HttpStatus.CREATED).body(response);
    // }

    // @ResponseBody
    // @PutMapping("/api/domain/roles/{roleId}")
    // public ResponseEntity<RoleResponse> update(
    //     @PathVariable("roleId") String roleId,
    //     @Valid @RequestBody RoleRequest request
    // ) {
    //     RoleResponse response = service.update(roleId, request);
    //     return ResponseEntity.ok(response);
    // }

    // @ResponseBody
    // @DeleteMapping("/api/domain/roles/{roleId}")
    // public ResponseEntity<Void> delete(@PathVariable("roleId") String roleId) {
    //     service.delete(roleId);
    //     return ResponseEntity.noContent().build();
    // }
}
