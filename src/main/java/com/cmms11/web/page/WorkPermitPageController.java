package com.cmms11.web.page;

import com.cmms11.code.CodeService;
import com.cmms11.domain.dept.DeptService;
import com.cmms11.domain.site.SiteService;
import com.cmms11.workpermit.WorkPermitResponse;
import com.cmms11.workpermit.WorkPermitService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 이름: WorkPermitPageController
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 작업허가 관리 페이지 컨트롤러 (HTML 반환)
 */
@Controller
public class WorkPermitPageController {

    private final WorkPermitService service;
    private final CodeService codeService;
    private final SiteService siteService;
    private final DeptService deptService;

    public WorkPermitPageController(
        WorkPermitService service,
        CodeService codeService,
        SiteService siteService,
        DeptService deptService
    ) {
        this.service = service;
        this.codeService = codeService;
        this.siteService = siteService;
        this.deptService = deptService;
    }

    @GetMapping("/workpermit/list")
    public String list(
        @RequestParam(required = false) String permitId,
        @RequestParam(required = false) String plantId,
        @RequestParam(required = false) String jobId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String stage,
        @RequestParam(required = false) String plannedDateFrom,
        @RequestParam(required = false) String plannedDateTo,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Pageable pageable,
        Model model
    ) {
        Page<WorkPermitResponse> page = service.list(
            permitId, plantId, jobId, status, stage, plannedDateFrom, plannedDateTo, pageable
        );
        
        model.addAttribute("page", page);
        model.addAttribute("permitId", permitId);
        model.addAttribute("plantId", plantId);
        model.addAttribute("jobId", jobId);
        model.addAttribute("status", status);
        model.addAttribute("stage", stage);
        model.addAttribute("plannedDateFrom", plannedDateFrom);
        model.addAttribute("plannedDateTo", plannedDateTo);
        
        // 필터용 코드 데이터 추가
        try {
            model.addAttribute("permitTypes", codeService.listItems("PERMT", null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("permitTypes", java.util.List.of());
        }
        
        try {
            model.addAttribute("statusList", codeService.listItems("APPRV", null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("statusList", java.util.List.of());
        }
        
        return _fragment ? "workpermit/list :: content" : "workpermit/list";
    }

    @GetMapping("/workpermit/detail/{permitId}")
    public String detail(
        @PathVariable String permitId,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Model model
    ) {
        WorkPermitResponse workPermit = service.get(permitId);
        model.addAttribute("workPermit", workPermit);
        
        return _fragment ? "workpermit/detail :: content" : "workpermit/detail";
    }

    /**
     * 작업허가 등록/수정 폼 페이지
     * 
     * 참고: WorkPermit은 PLN(계획) 단계만 존재하므로 copyForActual() 메서드는 포함되지 않음.
     * refEntity, refId, refStage 파라미터는 향후 확장성을 위해 인터페이스에 포함하였으나 현재는 미사용.
     */
    @GetMapping("/workpermit/form")
    public String form(
        @RequestParam(required = false) String id,
        @RequestParam(required = false) String stage,
        @RequestParam(required = false) String refEntity,  // 향후 확장성 위해 유지 (현재 미사용)
        @RequestParam(required = false) String refId,      // 향후 확장성 위해 유지 (현재 미사용)
        @RequestParam(required = false) String refStage,   // 향후 확장성 위해 유지 (현재 미사용)
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Model model
    ) {
        boolean isNew = (id == null || id.isEmpty());
        WorkPermitResponse workPermit;
        
        if (isNew) {
            workPermit = createEmptyWorkPermit(stage);
            
            // 참조 정보가 있으면 설정 (실적 입력 시 계획 복사)
            if (refId != null && !refId.isEmpty()) {
                WorkPermitResponse refWorkPermit = service.get(refId);
                workPermit = copyForActual(refWorkPermit, refEntity, refId, refStage);
            }
        } else {
            workPermit = service.get(id);
        }
        
        model.addAttribute("workPermit", workPermit);
        model.addAttribute("isNew", isNew);
        model.addAttribute("stage", stage != null ? stage : "PLN");
        
        addReferenceData(model);
        
        return _fragment ? "workpermit/form :: content" : "workpermit/form";
    }

    /**
     * 빈 WorkPermit 객체 생성 (신규 등록용)
     */
    private WorkPermitResponse createEmptyWorkPermit(String stage) {
        return new WorkPermitResponse(
            null,  // permitId
            null,  // name
            null,  // plantId
            null,  // jobId
            null,  // siteId
            null,  // deptId
            null,  // memberId
            null,  // plannedDate
            null,  // actualDate
            null,  // workSummary
            null,  // hazardFactor
            null,  // safetyFactor
            null,  // checksheetJson
            "DRAFT",  // status - 기본값
            stage != null ? stage : "PLN",  // stage - 기본값 "PLN"
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
    private WorkPermitResponse copyForActual(
        WorkPermitResponse plan,
        String refEntity,
        String refId,
        String refStage
    ) {
        return new WorkPermitResponse(
            null,  // permitId는 null (autoNumberService가 생성)
            plan.name(),
            plan.plantId(),
            plan.jobId(),
            plan.siteId(),
            plan.deptId(),
            plan.memberId(),
            plan.plannedDate(),
            plan.actualDate(),
            plan.workSummary(),
            plan.hazardFactor(),
            plan.safetyFactor(),
            plan.checksheetJson(),
            "DRAFT",  // status는 DRAFT
            "ACT",  // stage를 ACT로 변경
            refEntity,  // 참조 엔티티 (PERM)
            refId,      // 참조 ID (계획의 permitId)
            refStage,   // 참조 단계 (PLN)
            null,  // approvalId는 null
            null,  // fileGroupId는 null
            plan.note(),
            null, null, null, null,
            plan.items()  // 항목들은 복사
        );
    }

    private void addReferenceData(Model model) {
        try {
            model.addAttribute("jobTypes", codeService.listItems("JOBTP", null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("jobTypes", java.util.List.of());
        }
        
        try {
            model.addAttribute("permitTypes", codeService.listItems("PERMT", null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("permitTypes", java.util.List.of());
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
}

