package com.cmms11.web.page;

import com.cmms11.code.CodeService;
import com.cmms11.domain.dept.DeptService;
import com.cmms11.domain.site.SiteService;
import com.cmms11.inspection.InspectionResponse;
import com.cmms11.inspection.InspectionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 이름: InspectionPageController
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 점검 관리 페이지 컨트롤러 (HTML 반환)
 */
@Controller
public class InspectionPageController {

    private final InspectionService service;
    private final CodeService codeService;
    private final SiteService siteService;
    private final DeptService deptService;

    public InspectionPageController(
        InspectionService service,
        CodeService codeService,
        SiteService siteService,
        DeptService deptService
    ) {
        this.service = service;
        this.codeService = codeService;
        this.siteService = siteService;
        this.deptService = deptService;
    }

    /**
     * 점검 목록 페이지
     */
    @GetMapping("/inspection/list")
    public String list(
        @RequestParam(required = false) String inspectionId,
        @RequestParam(required = false) String plantId,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String stage,
        @RequestParam(required = false) String plannedDateFrom,
        @RequestParam(required = false) String plannedDateTo,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Pageable pageable,
        Model model
    ) {
        Page<InspectionResponse> page = service.list(
            inspectionId, plantId, name, status, stage, plannedDateFrom, plannedDateTo, pageable
        );
        
        model.addAttribute("page", page);
        model.addAttribute("inspectionId", inspectionId);
        model.addAttribute("plantId", plantId);
        model.addAttribute("name", name);
        model.addAttribute("status", status);
        model.addAttribute("stage", stage);
        model.addAttribute("plannedDateFrom", plannedDateFrom);
        model.addAttribute("plannedDateTo", plannedDateTo);
        
        // 필터용 코드 데이터 추가
        try {
            model.addAttribute("statusList", codeService.listItems("APPRV", null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("statusList", java.util.List.of());
        }
        
        return _fragment ? "inspection/list :: content" : "inspection/list";
    }

    /**
     * 점검 상세 페이지
     */
    @GetMapping("/inspection/detail/{inspectionId}")
    public String detail(
        @PathVariable String inspectionId,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Model model
    ) {
        InspectionResponse inspection = service.get(inspectionId);
        model.addAttribute("inspection", inspection);
        
        return _fragment ? "inspection/detail :: content" : "inspection/detail";
    }

    /**
     * 점검 등록/수정 폼 페이지
     */
    @GetMapping("/inspection/form")
    public String form(
        @RequestParam(required = false) String id,
        @RequestParam(required = false) String stage,
        @RequestParam(required = false) String refEntity,
        @RequestParam(required = false) String refId,
        @RequestParam(required = false) String refStage,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Model model
    ) {
        boolean isNew = (id == null || id.isEmpty());
        InspectionResponse inspection;
        
        if (isNew) {
            // 신규 등록
            inspection = createEmptyInspection(stage);
            
            // 참조 정보가 있으면 설정 (실적 입력 시 계획 복사)
            if (refId != null && !refId.isEmpty()) {
                InspectionResponse refInspection = service.get(refId);
                inspection = copyForActual(refInspection, refEntity, refId, refStage);  //Id, ref_* null 처리 됨.
            }
        } else {
            // 수정 모드
            inspection = service.get(id);
        }
        
        model.addAttribute("inspection", inspection);
        model.addAttribute("isNew", isNew);
        model.addAttribute("stage", stage != null ? stage : inspection.stage());
        
        // Select box용 참조 데이터 추가
        addReferenceData(model);
        
        return _fragment ? "inspection/form :: content" : "inspection/form";
    }

    /**
     * 점검 계획 페이지 (계획 전용)
     */
    @GetMapping("/inspection/plan")
    public String plan(
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Model model
    ) {
        // 계획 단계 데이터만 조회 (stage=PLN)
        Page<InspectionResponse> page = service.list(
            null, null, null, null, "PLN", null, null, Pageable.unpaged()
        );
        
        model.addAttribute("page", page);
        
        return _fragment ? "inspection/plan :: content" : "inspection/plan";
    }

    /**
     * Select box용 참조 데이터 추가
     */
    private void addReferenceData(Model model) {
        try {
            model.addAttribute("jobTypes", codeService.listItems("JOBTP", null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("jobTypes", java.util.List.of());
        }
        
        try {
            model.addAttribute("sites", siteService.list(null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("sites", java.util.List.of());
        }
        
        try {
            model.addAttribute("depts", deptService.list(null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("depts", java.util.List.of());
        }
    }

    /**
     * 빈 Inspection 객체 생성 (신규 등록용)
     */
    private InspectionResponse createEmptyInspection(String stage) {
        // 신규 등록 시 빈 객체 생성 (Thymeleaf null 참조 방지)
        return new InspectionResponse(
            null,  // inspectionId
            null,  // name
            null,  // plantId
            null,  // jobId
            null,  // siteId
            null,  // deptId
            null,  // memberId
            null,  // plannedDate
            null,  // actualDate
            "DRAFT",  // status - 기본값
            stage != null ? stage : "ACT",  // stage - 기본값
            null,  // refEntity
            null,  // refId
            null,  // refStage
            null,  // approvalId
            null,  // fileGroupId
            null,  // note
            null,  // createdAt
            null,  // createdBy
            null,  // updatedAt
            null,  // updatedBy
            java.util.List.of()  // items
        );
    }

    /**
     * 계획 복사하여 실적 생성용 객체 생성 (ID는 null)
     */
    private InspectionResponse copyForActual(
        InspectionResponse plan,
        String refEntity,
        String refId,
        String refStage
    ) {
        // 실적 입력 시 계획 데이터 복사, Service에서 ID 재발급
        // inspectionId를 null로 설정하여 autoNumberService가 새 ID를 생성하도록 함
        return new InspectionResponse(
            null,  // inspectionId는 null (autoNumberService가 생성)
            plan.name(),
            plan.plantId(),
            plan.jobId(),
            plan.siteId(),
            plan.deptId(),
            plan.memberId(),
            plan.plannedDate(),
            plan.actualDate(),
            "DRAFT",  // status는 DRAFT (실적은 새로운 승인 프로세스)
            "ACT",  // stage를 ACT로 변경
            refEntity,  // 참조 엔티티 (INSP)
            refId,      // 참조 ID (계획의 inspectionId)
            refStage,   // 참조 단계 (PLN)
            null,  // approvalId는 null (새로운 결재)
            null,  // fileGroupId는 null (새로운 파일)
            plan.note(),
            null,  // createdAt는 null (서버에서 설정)
            null,  // createdBy는 null (서버에서 설정)
            null,  // updatedAt는 null (서버에서 설정)
            null,  // updatedBy는 null (서버에서 설정)
            plan.items()  // 항목들은 복사
        );
    }
}

