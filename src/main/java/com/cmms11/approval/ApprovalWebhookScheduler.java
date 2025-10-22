package com.cmms11.approval;

import com.cmms11.config.ApprovalWebhookProperties;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Outbox에 적재된 이벤트를 Webhook으로 발송한다.
 */
@Component
public class ApprovalWebhookScheduler {

    private static final Logger log = LoggerFactory.getLogger(ApprovalWebhookScheduler.class);
    private static final String SIGNATURE_HEADER = "X-Approval-Signature";
    private static final String EVENT_HEADER = "X-Approval-Event";
    private static final String IDEMPOTENCY_HEADER = "X-Approval-Idempotency-Key";

    private final ApprovalOutboxRepository outboxRepository;
    private final ApprovalWebhookLogRepository webhookLogRepository;
    private final ApprovalWebhookProperties properties;
    private final RestTemplate restTemplate;

    private SecretKeySpec secretKeySpec;

    public ApprovalWebhookScheduler(
        ApprovalOutboxRepository outboxRepository,
        ApprovalWebhookLogRepository webhookLogRepository,
        ApprovalWebhookProperties properties
    ) {
        this.outboxRepository = outboxRepository;
        this.webhookLogRepository = webhookLogRepository;
        this.properties = properties;
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    void init() {
        try {
            secretKeySpec = new SecretKeySpec(
                properties.getSecurity().getSecretKey().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
        } catch (Exception e) {
            throw new IllegalStateException("Webhook 시그니처 초기화에 실패했습니다.", e);
        }
    }

    @Scheduled(fixedDelayString = "${app.webhook.scheduler.delay-millis:5000}")
    @Transactional
    public void dispatchPendingEvents() {
        LocalDateTime now = LocalDateTime.now();
        List<ApprovalOutbox> events = outboxRepository
            .findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(ApprovalOutboxStatus.PENDING, now);

        if (events.isEmpty()) {
            return;
        }

        log.debug("Webhook 전송 대상 {}건 처리 시작", events.size());
        for (ApprovalOutbox event : events) {
            processEvent(event, now);
        }
    }

    private void processEvent(ApprovalOutbox event, LocalDateTime now) {
        String targetUrl = resolveCallbackUrl(event.getCallbackUrl());
        HttpHeaders headers = buildHeaders(event.getPayload(), event);
        HttpEntity<String> entity = new HttpEntity<>(event.getPayload(), headers);

        int maxAttempts = properties.getRetry().getMaxAttempts();
        long backoffMillis = properties.getRetry().getBackoffMillis();

        try {
            ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
            int statusCode = response.getStatusCode().value();
            boolean success = response.getStatusCode().is2xxSuccessful();

            saveLog(event, targetUrl, statusCode, response.getBody(), null, now);

            if (success) {
                markAsSent(event, now);
            } else if (response.getStatusCode().is4xxClientError()) {
                markAsFailed(event, now, "Client error: " + statusCode, maxAttempts, backoffMillis);
            } else {
                markForRetry(event, now, "Server error: " + statusCode, maxAttempts, backoffMillis);
            }
        } catch (RestClientException ex) {
            saveLog(event, targetUrl, null, null, ex.getMessage(), now);
            markForRetry(event, now, ex.getMessage(), maxAttempts, backoffMillis);
        }
    }

    private void markAsSent(ApprovalOutbox event, LocalDateTime now) {
        event.setStatus(ApprovalOutboxStatus.SENT);
        event.setUpdatedAt(now);
        event.setLastAttemptAt(now);
        event.setLastErrorMessage(null);
        outboxRepository.save(event);
        log.debug("Webhook 전송 성공: outboxId={}", event.getId());
    }

    private void markAsFailed(
        ApprovalOutbox event,
        LocalDateTime now,
        String errorMessage,
        int maxAttempts,
        long backoffMillis
    ) {
        event.setStatus(ApprovalOutboxStatus.FAILED);
        event.setLastErrorMessage(errorMessage);
        event.setLastAttemptAt(now);
        event.setUpdatedAt(now);
        event.setNextAttemptAt(now.plus(Duration.ofMillis(backoffMillis)));
        outboxRepository.save(event);
        log.warn("Webhook 전송 실패-재시도 중단: outboxId={}, error={}", event.getId(), errorMessage);
    }

    private void markForRetry(
        ApprovalOutbox event,
        LocalDateTime now,
        String errorMessage,
        int maxAttempts,
        long backoffMillis
    ) {
        int nextRetry = event.getRetryCount() + 1;
        event.setRetryCount(nextRetry);
        event.setLastErrorMessage(errorMessage);
        event.setLastAttemptAt(now);
        event.setUpdatedAt(now);

        if (nextRetry >= maxAttempts) {
            event.setStatus(ApprovalOutboxStatus.FAILED);
            log.error("Webhook 전송 실패-재시도 한계 초과: outboxId={}, error={}", event.getId(), errorMessage);
        } else {
            event.setStatus(ApprovalOutboxStatus.PENDING);
            long delay = backoffMillis * Math.max(1, nextRetry);
            event.setNextAttemptAt(now.plus(Duration.ofMillis(delay)));
            log.warn("Webhook 전송 오류-재시도 예약: outboxId={}, retryCount={}, error={}", event.getId(), nextRetry, errorMessage);
        }

        outboxRepository.save(event);
    }

    private void saveLog(
        ApprovalOutbox event,
        String targetUrl,
        Integer statusCode,
        String responseBody,
        String errorMessage,
        LocalDateTime now
    ) {
        ApprovalWebhookLog logEntry = new ApprovalWebhookLog();
        logEntry.setOutboxId(event.getId());
        logEntry.setCompanyId(event.getCompanyId());
        logEntry.setApprovalId(event.getApprovalId());
        logEntry.setWebhookUrl(targetUrl);
        logEntry.setHttpStatus(statusCode);
        logEntry.setResponseBody(responseBody);
        logEntry.setErrorMessage(errorMessage);
        logEntry.setCreatedAt(now);
        webhookLogRepository.save(logEntry);
    }

    private HttpHeaders buildHeaders(String payload, ApprovalOutbox event) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(EVENT_HEADER, event.getEventType().name());
        if (event.getIdempotencyKey() != null) {
            headers.add(IDEMPOTENCY_HEADER, event.getIdempotencyKey());
        }
        headers.add(SIGNATURE_HEADER, sign(payload));
        return headers;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            byte[] data = payload.getBytes(StandardCharsets.UTF_8);
            byte[] signature = mac.doFinal(data);
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            throw new IllegalStateException("Webhook 시그니처 생성에 실패했습니다.", e);
        }
    }

    private String resolveCallbackUrl(String callbackUrl) {
        if (callbackUrl == null || callbackUrl.isBlank()) {
            throw new IllegalStateException("Callback URL이 비어 있습니다.");
        }
        if (callbackUrl.startsWith("http://") || callbackUrl.startsWith("https://")) {
            return callbackUrl;
        }
        String base = properties.getCallbackBase();
        if (base.endsWith("/") && callbackUrl.startsWith("/")) {
            return base.substring(0, base.length() - 1) + callbackUrl;
        } else if (!base.endsWith("/") && !callbackUrl.startsWith("/")) {
            return base + "/" + callbackUrl;
        }
        return base + callbackUrl;
    }
}
