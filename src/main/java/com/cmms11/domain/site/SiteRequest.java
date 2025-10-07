package com.cmms11.domain.site;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 이름: SiteRequest
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 사업장 생성/수정 요청 DTO.
 */
public record SiteRequest(
    @NotBlank @Size(max = 5) String siteId,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 5) String companyId,
    @Size(max = 30) String phone,
    @Size(max = 200) String address,
    @Size(max = 500) String note
) {
}

