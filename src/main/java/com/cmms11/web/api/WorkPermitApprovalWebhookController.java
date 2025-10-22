package com.cmms11.web.api;

import com.cmms11.approval.ApprovalEventPayload;
import com.cmms11.approval.ApprovalEventType;
import com.cmms11.approval.WebhookIdempotency;
import com.cmms11.approval.WebhookIdempotencyId;
import com.cmms11.approval.WebhookIdempotencyRepository;
import com.cmms11.approval.client.ApprovalStatusTransition;
import com.cmms11.config.ApprovalWebhookProperties;
import com.cmms11.workpermit.WorkPermitApprovalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/work-permits/approvals")
public class WorkPermitApprovalWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WorkPermitApprovalWebhookController.class);

    private final WorkPermitApprovalService approvalService;
    private final WebhookIdempotencyRepository idempotencyRepository;
    private final ApprovalWebhookProperties webhookProperties;
    private final ObjectMapper objectMapper;

    public WorkPermitApprovalWebhookController(
        WorkPermitApprovalService approvalService,
        WebhookIdempotencyRepository idempotencyRepository,
        ApprovalWebhookProperties webhookProperties,
        ObjectMapper objectMapper
    ) {
        this.approvalService = approvalService;
        this.idempotencyRepository = idempotencyRepository;
        this.webhookProperties = webhookProperties;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
        @RequestHeader(value = "X-Approval-Signature", required = false) String signatureHeader,
        @RequestHeader(value = "X-Approval-Event", required = false) String eventHeader,
        @RequestHeader(value = "X-Approval-Idempotency-Key", required = false) String idempotencyHeader,
        @RequestBody byte[] body,
        HttpServletRequest request
    ) throws IOException {

        if (signatureHeader == null || signatureHeader.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            verifySignature(signatureHeader, body);
        } catch (IllegalStateException ex) {
            log.warn("WorkPermit webhook signature verification failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ApprovalEventPayload payload = objectMapper.readValue(body, ApprovalEventPayload.class);
        ApprovalEventType eventType = payload.eventType();
        if (eventType == null && eventHeader != null) {
            eventType = ApprovalEventType.valueOf(eventHeader);
        }

        if (eventType == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        String idempotencyKey = payload.idempotencyKey() != null ? payload.idempotencyKey() : idempotencyHeader;
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        WebhookIdempotencyId id = new WebhookIdempotencyId(payload.companyId(), idempotencyKey);
        if (idempotencyRepository.findById(id).isPresent()) {
            return ResponseEntity.ok().build();
        }

        applyStatus(payload, eventType);

        WebhookIdempotency entity = new WebhookIdempotency();
        entity.setId(id);
        entity.setProcessedAt(LocalDateTime.now());
        idempotencyRepository.save(entity);

        return ResponseEntity.ok().build();
    }

    private void verifySignature(String signatureHeader, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                webhookProperties.getSecurity().getSecretKey().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(keySpec);
            String expected = Base64.getEncoder().encodeToString(mac.doFinal(body));
            if (!expected.equals(signatureHeader)) {
                throw new IllegalStateException("Invalid webhook signature");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Webhook signature verification failed", e);
        }
    }

    private void applyStatus(ApprovalEventPayload payload, ApprovalEventType eventType) {
        ApprovalStatusTransition transition = switch (eventType) {
            case APPROVED -> ApprovalStatusTransition.APPROVED;
            case REJECTED -> ApprovalStatusTransition.REJECTED;
            case CANCELLED -> ApprovalStatusTransition.CANCELLED;
            default -> null;
        };

        if (transition != null) {
            approvalService.applyApprovalStatus(payload.refId(), payload.refStage(), transition);
        }
    }
}
