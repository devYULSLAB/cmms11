package com.cmms11.approval;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 이름: Approval
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 결재 헤더 엔티티.
 */
@Entity
@Table(name = "approval")
@Getter
@Setter
@NoArgsConstructor
public class Approval {

    @EmbeddedId
    private ApprovalId id;

    @Column(length = 100)
    private String title;

    @Column(length = 10)
    private String status;

    @Column(name = "ref_entity", length = 64)
    private String refEntity;

    @Column(name = "ref_id", length = 10)
    private String refId;

    @Lob
    @Column
    private String content;

    @Column(name = "file_group_id", length = 10)
    private String fileGroupId;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 10)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 10)
    private String updatedBy;

    // 편의 메서드들
    public String getApprovalId() {
        return id != null ? id.getApprovalId() : null;
    }
    
    public String getCompanyId() {
        return id != null ? id.getCompanyId() : null;
    }
}
