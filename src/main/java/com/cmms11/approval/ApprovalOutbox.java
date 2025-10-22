package com.cmms11.approval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Webhook 발송을 위한 Outbox 이벤트 엔티티.
 */
@Entity
@Table(name = "approval_outbox")
@Getter
@Setter
@NoArgsConstructor
public class ApprovalOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", length = 5, nullable = false)
    private String companyId;

    @Column(name = "approval_id", length = 10, nullable = false)
    private String approvalId;

    @Column(name = "callback_url", length = 255, nullable = false)
    private String callbackUrl;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 20, nullable = false)
    private ApprovalEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private ApprovalOutboxStatus status = ApprovalOutboxStatus.PENDING;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "last_error_message", length = 500)
    private String lastErrorMessage;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
