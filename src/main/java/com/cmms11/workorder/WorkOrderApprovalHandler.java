package com.cmms11.workorder;

import com.cmms11.approval.handler.ApprovalRefHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 이름: WorkOrderApprovalHandler
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 작업지시 결재 상태 변경을 처리하는 핸들러.
 *               ApprovalService → WorkOrderService 순환 참조를 제거합니다.
 */
@Component
public class WorkOrderApprovalHandler implements ApprovalRefHandler {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderApprovalHandler.class);

    private final WorkOrderService workOrderService;

    public WorkOrderApprovalHandler(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @Override
    public boolean supports(String refEntity, String refStage) {
        return "WORK".equals(refEntity);
    }

    @Override
    public void handle(String action, String refId, String refStage) {
        log.debug("WorkOrderApprovalHandler: action={}, refId={}, refStage={}", action, refId, refStage);

        // PLN 단계 처리
        if ("PLN".equals(refStage)) {
            switch (action) {
                case "APPRV":
                    workOrderService.onPlanApprovalApprove(refId);
                    break;
                case "REJCT":
                    workOrderService.onPlanApprovalReject(refId);
                    break;
                case "DELETE":
                    workOrderService.onPlanApprovalDelete(refId);
                    break;
                case "CMPLT":
                    workOrderService.onPlanApprovalComplete(refId);
                    break;
                default:
                    log.warn("Unknown action for PLN stage: {}", action);
            }
        }
        // ACT 단계 처리
        else if ("ACT".equals(refStage)) {
            switch (action) {
                case "APPRV":
                    workOrderService.onActualApprovalApprove(refId);
                    break;
                case "REJCT":
                    workOrderService.onActualApprovalReject(refId);
                    break;
                case "DELETE":
                    workOrderService.onActualApprovalDelete(refId);
                    break;
                case "CMPLT":
                    workOrderService.onActualApprovalComplete(refId);
                    break;
                default:
                    log.warn("Unknown action for ACT stage: {}", action);
            }
        } else {
            log.warn("Unknown refStage for WorkOrder: {}", refStage);
        }
    }
}

