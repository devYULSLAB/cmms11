package com.cmms11.approval;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox 모니터링 및 재시도 기능.
 */
@Service
public class ApprovalMonitoringService {

    private final ApprovalOutboxRepository outboxRepository;

    public ApprovalMonitoringService(ApprovalOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional(readOnly = true)
    public ApprovalOutboxStatusResponse getOutboxStatus() {
        long pending = outboxRepository.countByStatus(ApprovalOutboxStatus.PENDING);
        long failed = outboxRepository.countByStatus(ApprovalOutboxStatus.FAILED);
        LocalDateTime oldestPending = outboxRepository
            .findTop1ByStatusOrderByCreatedAtAsc(ApprovalOutboxStatus.PENDING)
            .map(ApprovalOutbox::getCreatedAt)
            .orElse(null);
        return new ApprovalOutboxStatusResponse(pending, failed, oldestPending);
    }

    @Transactional(readOnly = true)
    public List<ApprovalOutboxEventResponse> getFailedEvents(int limit) {
        List<ApprovalOutbox> events = outboxRepository
            .findTop20ByStatusOrderByUpdatedAtDesc(ApprovalOutboxStatus.FAILED);
        return events.stream()
            .limit(limit)
            .map(ApprovalOutboxEventResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public void retry(Long outboxId) {
        ApprovalOutbox outbox = outboxRepository
            .findById(outboxId)
            .orElseThrow(() -> new IllegalArgumentException("Outbox 이벤트를 찾을 수 없습니다: " + outboxId));

        outbox.setStatus(ApprovalOutboxStatus.PENDING);
        outbox.setLastErrorMessage(null);
        outbox.setLastAttemptAt(null);
        outbox.setNextAttemptAt(LocalDateTime.now());
        outbox.setUpdatedAt(LocalDateTime.now());
        outboxRepository.save(outbox);
    }
}
