package com.cmms11.approval;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 이름: ApprovalRepository
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 결재 헤더에 대한 CRUD 및 검색 레포지토리.
 */
public interface ApprovalRepository extends JpaRepository<Approval, ApprovalId> {

    Page<Approval> findByIdCompanyId(String companyId, Pageable pageable);

    @Query(
        "select a from Approval a " +
        "where a.id.companyId = :companyId and (a.id.approvalId like :keyword or a.title like :keyword)"
    )
    Page<Approval> search(
        @Param("companyId") String companyId,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Query(
        "select a from Approval a " +
        "where a.id.companyId = :companyId " +
        "and (:title is null or :title = '' or a.title like concat('%', :title, '%')) " +
        "and (:createdBy is null or :createdBy = '' or a.createdBy like concat('%', :createdBy, '%')) " +
        "and (:status is null or :status = '' or a.status = :status)"
    )
    Page<Approval> findByFilters(
        @Param("companyId") String companyId,
        @Param("title") String title,
        @Param("createdBy") String createdBy,
        @Param("status") String status,
        Pageable pageable
    );

    Optional<Approval> findByIdCompanyIdAndIdApprovalId(String companyId, String approvalId);

    // 결재함 조회 쿼리
    
    /**
     * 미결함: 내가 결재해야 할 문서
     */
    @Query(
        "SELECT DISTINCT a FROM Approval a " +
        "JOIN ApprovalStep s ON a.id.companyId = s.id.companyId AND a.id.approvalId = s.id.approvalId " +
        "WHERE a.id.companyId = :companyId " +
        "AND s.memberId = :memberId " +
        "AND s.decidedAt IS NULL " +
        "AND a.status IN ('SUBMIT', 'PROC') " +
        "ORDER BY a.createdAt DESC"
    )
    Page<Approval> findPendingByMemberId(
        @Param("companyId") String companyId,
        @Param("memberId") String memberId,
        Pageable pageable
    );

    /**
     * 기결함: 내가 승인/합의한 문서
     */
    @Query(
        "SELECT DISTINCT a FROM Approval a " +
        "JOIN ApprovalStep s ON a.id.companyId = s.id.companyId AND a.id.approvalId = s.id.approvalId " +
        "WHERE a.id.companyId = :companyId " +
        "AND s.memberId = :memberId " +
        "AND s.decidedAt IS NOT NULL " +
        "AND s.result = 'APPROVE' " +
        "ORDER BY s.decidedAt DESC"
    )
    Page<Approval> findApprovedByMemberId(
        @Param("companyId") String companyId,
        @Param("memberId") String memberId,
        Pageable pageable
    );

    /**
     * 반려함: 내가 반려한 문서
     */
    @Query(
        "SELECT DISTINCT a FROM Approval a " +
        "JOIN ApprovalStep s ON a.id.companyId = s.id.companyId AND a.id.approvalId = s.id.approvalId " +
        "WHERE a.id.companyId = :companyId " +
        "AND s.memberId = :memberId " +
        "AND s.decidedAt IS NOT NULL " +
        "AND s.result = 'REJECT' " +
        "ORDER BY s.decidedAt DESC"
    )
    Page<Approval> findRejectedByMemberId(
        @Param("companyId") String companyId,
        @Param("memberId") String memberId,
        Pageable pageable
    );

    /**
     * 상신함: 내가 상신한 문서
     */
    @Query(
        "SELECT a FROM Approval a " +
        "WHERE a.id.companyId = :companyId " +
        "AND a.createdBy = :memberId " +
        "AND a.status IN ('SUBMIT', 'PROC', 'APPROV', 'REJECT') " +
        "ORDER BY a.createdAt DESC"
    )
    Page<Approval> findSentByMemberId(
        @Param("companyId") String companyId,
        @Param("memberId") String memberId,
        Pageable pageable
    );
}
