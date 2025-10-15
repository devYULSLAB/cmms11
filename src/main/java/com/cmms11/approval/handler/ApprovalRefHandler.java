package com.cmms11.approval.handler;

/**
 * 이름: ApprovalRefHandler
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 결재 참조 모듈(Inspection, WorkOrder, WorkPermit 등)의 
 *               상태 업데이트를 처리하는 핸들러 인터페이스.
 *               전략 패턴으로 순환 참조를 제거합니다.
 */
public interface ApprovalRefHandler {

    /**
     * 이 핸들러가 특정 refEntity와 refStage를 지원하는지 확인
     * 
     * @param refEntity 참조 엔티티 ("INSP", "WORK", "WPER" 등)
     * @param refStage 참조 단계 ("PLN", "ACT" 등)
     * @return 지원 여부
     */
    boolean supports(String refEntity, String refStage);

    /**
     * 결재 액션에 따라 참조 모듈의 상태를 업데이트
     * 
     * @param action 결재 액션 ("APPRV": 승인, "REJCT": 반려, "DELETE": 삭제, "CMPLT": 확정)
     * @param refId 참조 엔티티 ID
     * @param refStage 참조 단계 ("PLN", "ACT" 등)
     */
    void handle(String action, String refId, String refStage);
}

