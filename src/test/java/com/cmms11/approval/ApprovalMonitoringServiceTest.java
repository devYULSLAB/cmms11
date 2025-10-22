package com.cmms11.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApprovalMonitoringServiceTest {

    private ApprovalOutboxRepository outboxRepository;
    private ApprovalMonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(ApprovalOutboxRepository.class);
        monitoringService = new ApprovalMonitoringService(outboxRepository);
    }

    @Test
    void getOutboxStatusReturnsCountsAndOldestPending() {
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(15);

        when(outboxRepository.countByStatus(ApprovalOutboxStatus.PENDING)).thenReturn(3L);
        when(outboxRepository.countByStatus(ApprovalOutboxStatus.FAILED)).thenReturn(1L);

        ApprovalOutbox oldest = new ApprovalOutbox();
        oldest.setCreatedAt(createdAt);

        when(outboxRepository.findTop1ByStatusOrderByCreatedAtAsc(ApprovalOutboxStatus.PENDING))
            .thenReturn(Optional.of(oldest));

        ApprovalOutboxStatusResponse response = monitoringService.getOutboxStatus();

        assertThat(response.pendingCount()).isEqualTo(3);
        assertThat(response.failedCount()).isEqualTo(1);
        assertThat(response.oldestPendingCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void getFailedEventsMapsRepositoryResults() {
        ApprovalOutbox failed = new ApprovalOutbox();
        failed.setId(10L);
        failed.setApprovalId("A0001");
        failed.setEventType(ApprovalEventType.SUBMITTED);
        failed.setStatus(ApprovalOutboxStatus.FAILED);
        failed.setRetryCount(2);
        failed.setLastErrorMessage("timeout");
        failed.setCreatedAt(LocalDateTime.now().minusHours(1));
        failed.setUpdatedAt(LocalDateTime.now().minusMinutes(5));

        when(outboxRepository.findTop20ByStatusOrderByUpdatedAtDesc(ApprovalOutboxStatus.FAILED))
            .thenReturn(List.of(failed));

        List<ApprovalOutboxEventResponse> responses = monitoringService.getFailedEvents(5);

        assertThat(responses).hasSize(1);
        ApprovalOutboxEventResponse eventResponse = responses.get(0);
        assertThat(eventResponse.id()).isEqualTo(10L);
        assertThat(eventResponse.status()).isEqualTo(ApprovalOutboxStatus.FAILED);
        assertThat(eventResponse.retryCount()).isEqualTo(2);
    }

    @Test
    void retryResetsEventStatusAndSchedulesNextAttempt() {
        ApprovalOutbox event = new ApprovalOutbox();
        event.setId(99L);
        event.setStatus(ApprovalOutboxStatus.FAILED);
        event.setRetryCount(3);
        event.setNextAttemptAt(null);

        when(outboxRepository.findById(99L)).thenReturn(Optional.of(event));

        monitoringService.retry(99L);

        assertThat(event.getStatus()).isEqualTo(ApprovalOutboxStatus.PENDING);
        assertThat(event.getLastErrorMessage()).isNull();
        assertThat(event.getLastAttemptAt()).isNull();
        assertThat(event.getNextAttemptAt()).isNotNull();
        verify(outboxRepository).save(event);
    }
}
