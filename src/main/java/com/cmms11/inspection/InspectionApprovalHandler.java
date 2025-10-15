package com.cmms11.inspection;

import com.cmms11.approval.handler.ApprovalRefHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 이름: InspectionApprovalHandler
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 예방점검 결재 상태 변경을 처리하는 핸들러.
 *               ApprovalService → InspectionService 순환 참조를 제거합니다.
 */
@Component
public class InspectionApprovalHandler implements ApprovalRefHandler {

    private static final Logger log = LoggerFactory.getLogger(InspectionApprovalHandler.class);

    private final InspectionService inspectionService;

    public InspectionApprovalHandler(InspectionService inspectionService) {
        this.inspectionService = inspectionService;
    }

    @Override
    public boolean supports(String refEntity, String refStage) {
        return "INSP".equals(refEntity);
    }

    @Override
    public void handle(String action, String refId, String refStage) {
        log.debug("InspectionApprovalHandler: action={}, refId={}, refStage={}", action, refId, refStage);

        // PLN 단계 처리
        if ("PLN".equals(refStage)) {
            switch (action) {
                case "APPRV":
                    inspectionService.onPlanApprovalApprove(refId);
                    break;
                case "REJCT":
                    inspectionService.onPlanApprovalReject(refId);
                    break;
                case "DELETE":
                    inspectionService.onPlanApprovalDelete(refId);
                    break;
                case "CMPLT":
                    inspectionService.onPlanApprovalComplete(refId);
                    break;
                default:
                    log.warn("Unknown action for PLN stage: {}", action);
            }
        }
        // ACT 단계 처리
        else if ("ACT".equals(refStage)) {
            switch (action) {
                case "APPRV":
                    inspectionService.onActualApprovalApprove(refId);
                    break;
                case "REJCT":
                    inspectionService.onActualApprovalReject(refId);
                    break;
                case "DELETE":
                    inspectionService.onActualApprovalDelete(refId);
                    break;
                case "CMPLT":
                    inspectionService.onActualApprovalComplete(refId);
                    break;
                default:
                    log.warn("Unknown action for ACT stage: {}", action);
            }
        } else {
            log.warn("Unknown refStage for Inspection: {}", refStage);
        }
    }
}

