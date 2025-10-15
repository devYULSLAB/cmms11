package com.cmms11.web.page;

import com.cmms11.code.CodeService;
import com.cmms11.domain.dept.DeptService;
import com.cmms11.domain.site.SiteService;
import com.cmms11.workorder.WorkOrderResponse;
import com.cmms11.workorder.WorkOrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 이름: WorkOrderPageController
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 작업지시 관리 페이지 컨트롤러 (HTML 반환)
 */
@Controller
public class WorkOrderPageController {

    private final WorkOrderService service;
    private final CodeService codeService;
    private final SiteService siteService;
    private final DeptService deptService;

    public WorkOrderPageController(
        WorkOrderService service,
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
     * 작업지시 목록 페이지
     */
    @GetMapping("/workorder/list")
    public String list(
        @RequestParam(required = false) String orderId,
        @RequestParam(required = false) String plantId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String stage,
        @RequestParam(required = false) String plannedDateFrom,
        @RequestParam(required = false) String plannedDateTo,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Pageable pageable,
        Model model
    ) {
        Page<WorkOrderResponse> page = service.list(
            orderId, plantId, status, stage, plannedDateFrom, plannedDateTo, pageable
        );
        
        model.addAttribute("page", page);
        model.addAttribute("orderId", orderId);
        model.addAttribute("plantId", plantId);
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
        
        return _fragment ? "workorder/list :: content" : "workorder/list";
    }

    /**
     * 작업지시 상세 페이지
     */
    @GetMapping("/workorder/detail/{orderId}")
    public String detail(
        @PathVariable String orderId,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Model model
    ) {
        WorkOrderResponse workOrder = service.get(orderId);
        model.addAttribute("workOrder", workOrder);
        
        return _fragment ? "workorder/detail :: content" : "workorder/detail";
    }

    /**
     * 작업지시 등록/수정 폼 페이지
     */
    @GetMapping("/workorder/form")
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
        WorkOrderResponse workOrder;
        
        if (isNew) {
            // 신규 등록
            workOrder = createEmptyWorkOrder(stage);
            
            // 참조 정보가 있으면 설정 (실적 입력 시 계획 복사)
            if (refId != null && !refId.isEmpty()) {
                WorkOrderResponse refWorkOrder = service.get(refId);
                workOrder = copyForActual(refWorkOrder, refEntity, refId, refStage);  // ID, ref_* null 처리 됨.
            }
        } else {
            // 수정 모드
            workOrder = service.get(id);
        }
        
        model.addAttribute("workOrder", workOrder);
        model.addAttribute("isNew", isNew);
        model.addAttribute("stage", stage != null ? stage : workOrder.stage());
        
        // Select box용 참조 데이터 추가
        addReferenceData(model);
        
        return _fragment ? "workorder/form :: content" : "workorder/form";
    }

    /**
     * 빈 WorkOrder 객체 생성 (신규 등록용)
     */
    private WorkOrderResponse createEmptyWorkOrder(String stage) {
        return new WorkOrderResponse(
            null,  // orderId
            null,  // name
            null,  // plantId
            null,  // jobId
            null,  // siteId
            null,  // deptId
            null,  // memberId
            null,  // plannedDate
            null,  // plannedCost
            null,  // plannedLabor
            null,  // actualDate
            null,  // actualCost
            null,  // actualLabor
            "DRAFT",  // status - 기본값
            stage != null ? stage : "PLN",  // stage - 기본값 (WorkOrder는 PLN부터 시작)
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
    private WorkOrderResponse copyForActual(
        WorkOrderResponse plan,
        String refEntity,
        String refId,
        String refStage
    ) {
        // 실적 입력 시 계획 데이터 복사, Service에서 ID 재발급
        // orderId를 null로 설정하여 autoNumberService가 새 ID를 생성하도록 함
        return new WorkOrderResponse(
            null,  // orderId는 null (autoNumberService가 생성)
            plan.name(),
            plan.plantId(),
            plan.jobId(),
            plan.siteId(),
            plan.deptId(),
            plan.memberId(),
            plan.plannedDate(),
            plan.plannedCost(),
            plan.plannedLabor(),
            plan.actualDate(),
            plan.actualCost(),
            plan.actualLabor(),
            "DRAFT",  // status는 DRAFT (실적은 새로운 승인 프로세스)
            "ACT",  // stage를 ACT로 변경
            refEntity,  // 참조 엔티티 (WORD)
            refId,      // 참조 ID (계획의 orderId)
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
}

