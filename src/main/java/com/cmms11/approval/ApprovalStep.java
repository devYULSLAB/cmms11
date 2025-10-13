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
 * 이름: ApprovalStep
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 결재 단계 엔티티.
 */
@Entity
@Table(name = "approval_step")
@Getter
@Setter
@NoArgsConstructor
public class ApprovalStep {

    @EmbeddedId
    private ApprovalStepId id;

    @Column(name = "member_id", length = 5)
    private String memberId;

    @Column(length = 10)
    private String decision;  // 결재 역할: APPROVAL, AGREE, INFORM

    @Column(length = 10)
    private String result;    // 결재 결과: APPROVE, REJECT, PENDING (NULL)

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(length = 500)
    private String comment;
}
