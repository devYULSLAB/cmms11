package com.cmms11.approval;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Webhook 멱등 기록 레포지토리.
 */
public interface WebhookIdempotencyRepository extends JpaRepository<WebhookIdempotency, WebhookIdempotencyId> {

    Optional<WebhookIdempotency> findByIdCompanyIdAndIdIdempotencyKey(String companyId, String idempotencyKey);
}
