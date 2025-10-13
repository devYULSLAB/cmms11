package com.cmms11.approval;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 이름: ApprovalInboxRepository
 * 작성자: codex
 * 작성일: 2025-10-15
 * 프로그램 개요: approval_inbox 테이블 접근 레포지토리.
 */
public interface ApprovalInboxRepository extends JpaRepository<ApprovalInbox, ApprovalInboxId> {

    Page<ApprovalInbox> findByIdCompanyIdAndMemberId(String companyId, String memberId, Pageable pageable);

    Page<ApprovalInbox> findByIdCompanyIdAndMemberIdAndInboxType(
        String companyId,
        String memberId,
        String inboxType,
        Pageable pageable
    );

    Page<ApprovalInbox> findByIdCompanyIdAndMemberIdAndIsRead(
        String companyId,
        String memberId,
        String isRead,
        Pageable pageable
    );

    long countByIdCompanyIdAndMemberIdAndIsRead(String companyId, String memberId, String isRead);

    long countByIdCompanyIdAndMemberIdAndInboxType(String companyId, String memberId, String inboxType);

    Optional<ApprovalInbox> findByIdCompanyIdAndApprovalIdAndMemberId(
        String companyId,
        String approvalId,
        String memberId
    );

    List<ApprovalInbox> findByIdCompanyIdAndApprovalId(String companyId, String approvalId);

    void deleteByIdCompanyIdAndApprovalId(String companyId, String approvalId);
}
