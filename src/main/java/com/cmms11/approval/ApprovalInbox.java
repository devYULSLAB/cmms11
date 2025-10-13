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
 * 이름: ApprovalInbox
 * 작성자: codex
 * 작성일: 2025-10-15
 * 프로그램 개요: 결재 Inbox 엔티티.
 */
@Entity
@Table(name = "approval_inbox")
@Getter
@Setter
@NoArgsConstructor
public class ApprovalInbox {

    @EmbeddedId
    private ApprovalInboxId id;

    @Column(name = "member_id", length = 5)
    private String memberId;

    @Column(name = "approval_id", length = 10)
    private String approvalId;

    @Column(name = "step_no")
    private Integer stepNo;

    @Column(name = "inbox_type", length = 10)
    private String inboxType;

    @Column(name = "is_read", length = 1)
    private String isRead = "N";

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "notification_type", length = 20)
    private String notificationType;

    @Column(length = 100)
    private String title;

    @Column(name = "ref_entity", length = 64)
    private String refEntity;

    @Column(name = "ref_id", length = 10)
    private String refId;

    @Column(name = "submitted_by", length = 10)
    private String submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(length = 10)
    private String decision;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
