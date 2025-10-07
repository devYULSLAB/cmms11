package com.cmms11.domain.site;

import java.time.LocalDateTime;


/**
 * 이름: SiteResponse
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 사업장 조회 응답 DTO.
 */
public record SiteResponse(
    String siteId,
    String name,
    String companyId,
    String phone,
    String address,
    String note,
    String deleteMark,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
    String updatedBy
) {


    public static SiteResponse from(Site site) {
        return new SiteResponse(
            site.getId().getSiteId(),
            site.getName(),
            site.getId().getCompanyId(),
            site.getPhone(),
            site.getAddress(),
            site.getNote(),
            site.getDeleteMark(),
            site.getCreatedAt(),
            site.getCreatedBy(),
            site.getUpdatedAt(),
            site.getUpdatedBy()
        );
    }
}
