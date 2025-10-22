package com.cmms11.approval;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Webhook 발송 로그 레포지토리.
 */
public interface ApprovalWebhookLogRepository extends JpaRepository<ApprovalWebhookLog, Long> {

    List<ApprovalWebhookLog> findTop20ByApprovalIdOrderByCreatedAtDesc(String approvalId);
}
