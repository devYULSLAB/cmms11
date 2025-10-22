package com.cmms11.approval;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Outbox 이벤트 조회/저장 레포지토리.
 */
public interface ApprovalOutboxRepository extends JpaRepository<ApprovalOutbox, Long> {

    List<ApprovalOutbox> findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(
        ApprovalOutboxStatus status,
        LocalDateTime threshold
    );

    List<ApprovalOutbox> findTop50ByStatusOrderByCreatedAtAsc(ApprovalOutboxStatus status);

    long countByStatus(ApprovalOutboxStatus status);

    Optional<ApprovalOutbox> findTop1ByStatusOrderByCreatedAtAsc(ApprovalOutboxStatus status);

    List<ApprovalOutbox> findTop20ByStatusOrderByUpdatedAtDesc(ApprovalOutboxStatus status);
}
