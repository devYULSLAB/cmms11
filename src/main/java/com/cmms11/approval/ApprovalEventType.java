package com.cmms11.approval;

/**
 * 결재 이벤트 유형 정의.
 */
public enum ApprovalEventType {
    SUBMITTED("SUBMT"),
    APPROVED("APPRV"),
    REJECTED("REJCT"),
    CANCELLED("CNCL");

    private static final String CODE_TYPE = "APPRV";

    private final String statusCode;

    ApprovalEventType(String statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * DataInitializer에서 시드한 APPRV 코드 타입의 코드값과 매칭되는 상태 코드.
     */
    public String statusCode() {
        return statusCode;
    }

    /**
     * 공통 코드 타입 식별자.
     */
    public String codeType() {
        return CODE_TYPE;
    }
}
