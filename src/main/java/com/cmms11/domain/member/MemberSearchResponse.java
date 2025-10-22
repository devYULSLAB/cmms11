package com.cmms11.domain.member;

/**
 * 결재선 및 조직 선택에서 사용하는 경량 멤버 검색 결과 DTO.
 *
 * @param memberId  사번
 * @param name      이름
 * @param deptId    부서 ID
 * @param deptName  부서명
 * @param position  직책/직위
 * @param siteName  소속 사이트명
 * @param email     이메일
 */
public record MemberSearchResponse(
    String memberId,
    String name,
    String deptId,
    String deptName,
    String position,
    String siteName,
    String email
) {
}
