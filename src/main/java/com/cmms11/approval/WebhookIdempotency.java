package com.cmms11.approval;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Webhook 멱등 처리 기록.
 */
@Entity
@Table(name = "webhook_idempotency")
@Getter
@Setter
@NoArgsConstructor
public class WebhookIdempotency {

    @EmbeddedId
    private WebhookIdempotencyId id;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
}
