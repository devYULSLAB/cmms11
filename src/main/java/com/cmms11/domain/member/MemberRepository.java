package com.cmms11.domain.member;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, MemberId> {
    Optional<Member> findByIdCompanyIdAndIdMemberId(String companyId, String memberId);

    Page<Member> findByIdCompanyIdAndDeleteMark(String companyId, String deleteMark, Pageable pageable);

    @Query("select m from Member m where m.id.companyId=:companyId and m.deleteMark=:deleteMark and (m.id.memberId like :q or m.name like :q)")
    Page<Member> search(@Param("companyId") String companyId,
                        @Param("deleteMark") String deleteMark,
                        @Param("q") String q,
                        Pageable pageable);
    
    Page<Member> findByIdCompanyIdAndDeptIdAndDeleteMark(String companyId, String deptId, String deleteMark, Pageable pageable);

    @Query("""
        select new com.cmms11.domain.member.MemberSearchResponse(
            m.id.memberId,
            m.name,
            m.deptId,
            d.name,
            m.position,
            s.name,
            m.email
        )
        from Member m
        left join Dept d on d.id.companyId = m.id.companyId and d.id.deptId = m.deptId
        left join Site s on s.id.companyId = m.id.companyId and s.id.siteId = m.siteId
        where m.id.companyId = :companyId
          and m.deleteMark = 'N'
          and (:deptId is null or :deptId = '' or m.deptId = :deptId)
          and (
            :keyword is null or :keyword = '' or
            m.id.memberId like :keyword or
            m.name like :keyword
          )
        order by m.name asc, m.id.memberId asc
        """)
    Page<MemberSearchResponse> searchForApproval(
        @Param("companyId") String companyId,
        @Param("deptId") String deptId,
        @Param("keyword") String keyword,
        Pageable pageable
    );
}
