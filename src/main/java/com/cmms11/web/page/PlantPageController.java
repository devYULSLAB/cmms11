package com.cmms11.web.page;

import com.cmms11.plant.PlantRequest;
import com.cmms11.plant.PlantResponse;
import com.cmms11.plant.PlantService;
import com.cmms11.code.CodeService;
import com.cmms11.domain.site.SiteService;
import com.cmms11.domain.dept.DeptService;
import com.cmms11.domain.func.FuncService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 이름: PlantPageController
 * 작성자: codex
 * 작성일: 2025-10-15
 * 프로그램 개요: 설비 관리 페이지 컨트롤러 (HTML 반환)
 */
@Controller
public class PlantPageController {
    
    private final PlantService service;
    private final CodeService codeService;
    private final SiteService siteService;
    private final DeptService deptService;
    private final FuncService funcService;

    public PlantPageController(
        PlantService service,
        CodeService codeService,
        SiteService siteService,
        DeptService deptService,
        FuncService funcService
    ) {
        this.service = service;
        this.codeService = codeService;
        this.siteService = siteService;
        this.deptService = deptService;
        this.funcService = funcService;
    }

    /**
     * 설비 목록 페이지
     */
    @GetMapping("/plant/list")
    public String list(
        @RequestParam(name = "plantId", required = false) String plantId,
        @RequestParam(name = "name", required = false) String name,
        @RequestParam(name = "makerName", required = false) String makerName,
        @RequestParam(name = "funcId", required = false) String funcId,
        Pageable pageable,
        Model model
    ) {
        Page<PlantResponse> page = service.list(plantId, name, makerName, funcId, pageable);
        model.addAttribute("page", page);
        model.addAttribute("plantId", plantId);
        model.addAttribute("name", name);
        model.addAttribute("makerName", makerName);
        model.addAttribute("funcId", funcId);
        // 부서 목록 추가 (필터용)
        model.addAttribute("depts", deptService.list(null, Pageable.unpaged()).getContent());
        return "plant/list";
    }

    /**
     * 설비 등록 폼 페이지
     */
    @GetMapping("/plant/form")
    public String form(Model model) {
        model.addAttribute("plant", emptyPlant());
        model.addAttribute("isNew", true);
        addReferenceData(model);
        return "plant/form";
    }

    /**
     * 설비 수정 폼 페이지
     */
    @GetMapping("/plant/edit/{plantId}")
    public String edit(@PathVariable String plantId, Model model) {
        PlantResponse plant = service.get(plantId);
        model.addAttribute("plant", plant);
        model.addAttribute("isNew", false);
        addReferenceData(model);
        return "plant/form";
    }

    /**
     * 설비 상세 페이지
     */
    @GetMapping("/plant/detail/{plantId}")
    public String detail(@PathVariable String plantId, Model model) {
        PlantResponse plant = service.get(plantId);
        model.addAttribute("plant", plant);
        return "plant/detail";
    }

    /**
     * 설비 업로드 폼 페이지
     */
    @GetMapping("/plant/uploadForm")
    public String uploadForm(Model model) {
        return "plant/uploadForm";
    }

    /**
     * 설비 이력 페이지
     */
    @GetMapping("/plant/history")
    public String history(
        @RequestParam(name = "plantId", required = false) String plantId,
        @RequestParam(name = "plantName", required = false) String plantName,
        Model model
    ) {
        model.addAttribute("plantId", plantId);
        model.addAttribute("plantName", plantName);
        return "plant/history";
    }

    /**
     * 설비 저장 처리 (POST 방식)
     */
    @PostMapping("/plant/save")
    public String save(
        @ModelAttribute PlantRequest request,
        @RequestParam(required = false) String isNew
    ) {
        if ("true".equals(isNew)) {
            service.create(request);
        } else {
            service.update(request.plantId(), request);
        }
        return "redirect:/plant/list";
    }

    /**
     * 설비 삭제 처리 (POST 방식)
     */
    @PostMapping("/plant/delete/{plantId}")
    public String delete(@PathVariable String plantId) {
        service.delete(plantId);
        return "redirect:/plant/list";
    }

    /**
     * 빈 Plant 객체 생성 (신규 등록용)
     */
    private PlantResponse emptyPlant() {
        return new PlantResponse(
            null, // plantId
            null, // name
            null, // assetId
            null, // siteId
            null, // deptId
            null, // funcId
            null, // makerName
            null, // spec
            null, // model
            null, // serial
            null, // installDate
            null, // depreId
            null, // deprePeriod
            null, // purchaseCost
            null, // residualValue
            "N", // inspectionYn
            "N", // psmYn
            "N", // workpermitYn
            null, // inspectionInterval
            null, // lastInspection
            null, // nextInspection
            null, // fileGroupId
            null, // note
            "N", // deleteMark
            null, // createdAt
            null, // createdBy
            null, // updatedAt
            null  // updatedBy
        );
    }

    /**
     * Select box용 참조 데이터 추가
     */
    private void addReferenceData(Model model) {
        // 자산유형 (ASSET 코드 타입)
        try {
            model.addAttribute("assetTypes", codeService.listItems("ASSET", null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("assetTypes", List.of());
        }

        // 감가유형 (DEPRE 코드 타입)
        try {
            model.addAttribute("depreTypes", codeService.listItems("DEPRE", null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("depreTypes", List.of());
        }

        // 사업장 목록
        try {
            model.addAttribute("sites", siteService.list(null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("sites", List.of());
        }

        // 부서 목록
        try {
            model.addAttribute("depts", deptService.list(null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("depts", List.of());
        }

        // 기능위치 목록
        try {
            model.addAttribute("funcs", funcService.list(null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("funcs", List.of());
        }
    }
}

