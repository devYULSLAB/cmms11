package com.cmms11.domain.dept;

import java.time.LocalDateTime;

/**
 * 이름: DeptResponse
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 부서 조회 응답 DTO.
 */
public record DeptResponse(
    String deptId,
    String name,
    String companyId,
    String phone,
    String address,
    String parentDeptId,
    String note,
    String deleteMark,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
    String updatedBy
) {


    public static DeptResponse from(Dept dept) {
        return new DeptResponse(
            dept.getId().getDeptId(),
            dept.getName(),
            dept.getId().getCompanyId(),
            dept.getPhone(),
            dept.getAddress(),
            dept.getParentId(),
            dept.getNote(),
            dept.getDeleteMark(),
            dept.getCreatedAt(),
            dept.getCreatedBy(),
            dept.getUpdatedAt(),
            dept.getUpdatedBy()
        );
    }
}
