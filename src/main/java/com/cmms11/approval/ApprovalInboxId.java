package com.cmms11.approval;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 이름: ApprovalInboxId
 * 작성자: codex
 * 작성일: 2025-10-15
 * 프로그램 개요: approval_inbox 복합키 (company_id + inbox_id).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ApprovalInboxId implements Serializable {

    @Column(name = "company_id", length = 5, nullable = false)
    private String companyId;

    @Column(name = "inbox_id", length = 10, nullable = false)
    private String inboxId;

    public ApprovalInboxId(String companyId, String inboxId) {
        this.companyId = companyId;
        this.inboxId = inboxId;
    }
}
