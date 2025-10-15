package com.cmms11.workpermit;

import com.cmms11.approval.handler.ApprovalRefHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 이름: WorkPermitApprovalHandler
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 작업허가 결재 상태 변경을 처리하는 핸들러.
 *               ApprovalService → WorkPermitService 순환 참조를 제거합니다.
 */
@Component
public class WorkPermitApprovalHandler implements ApprovalRefHandler {

    private static final Logger log = LoggerFactory.getLogger(WorkPermitApprovalHandler.class);

    private final WorkPermitService workPermitService;

    public WorkPermitApprovalHandler(WorkPermitService workPermitService) {
        this.workPermitService = workPermitService;
    }

    @Override
    public boolean supports(String refEntity, String refStage) {
        return "WPER".equals(refEntity);
    }

    @Override
    public void handle(String action, String refId, String refStage) {
        log.debug("WorkPermitApprovalHandler: action={}, refId={}, refStage={}", action, refId, refStage);

        // WorkPermit은 PLN 단계만 존재
        if ("PLN".equals(refStage)) {
            switch (action) {
                case "APPRV":
                    workPermitService.onPlanApprovalApprove(refId);
                    break;
                case "REJCT":
                    workPermitService.onPlanApprovalReject(refId);
                    break;
                case "DELETE":
                    workPermitService.onPlanApprovalDelete(refId);
                    break;
                case "CMPLT":
                    workPermitService.onPlanApprovalComplete(refId);
                    break;
                default:
                    log.warn("Unknown action for PLN stage: {}", action);
            }
        } else {
            log.warn("Unknown refStage for WorkPermit: {} (only PLN is supported)", refStage);
        }
    }
}

