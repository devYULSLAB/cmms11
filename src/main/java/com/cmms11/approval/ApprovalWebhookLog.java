package com.cmms11.approval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Webhook 발송 로그.
 */
@Entity
@Table(name = "approval_webhook_log")
@Getter
@Setter
@NoArgsConstructor
public class ApprovalWebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outbox_id")
    private Long outboxId;

    @Column(name = "company_id", length = 5, nullable = false)
    private String companyId;

    @Column(name = "approval_id", length = 10, nullable = false)
    private String approvalId;

    @Column(name = "webhook_url", length = 255, nullable = false)
    private String webhookUrl;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Lob
    @Column(name = "response_body")
    private String responseBody;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
