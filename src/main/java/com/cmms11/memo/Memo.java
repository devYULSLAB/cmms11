package com.cmms11.memo;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "memo")
@Getter
@Setter
@NoArgsConstructor
public class Memo {

    @EmbeddedId
    private MemoId id;

    @Column(length = 100)
    private String title;

    @Lob
    @Column
    private String content;

    @Column(name = "plant_id", length = 10)
    private String plantId;

    @Column(name = "stage", length = 10)
    private String stage;

    @Column(name = "status", length = 10)
    private String status;

    @Column(name = "approval_id", length = 10)
    private String approvalId;

    @Column(name = "ref_entity", length = 64)
    private String refEntity;

    @Column(name = "ref_id", length = 10)
    private String refId;

    @Column(name = "ref_stage", length = 10)
    private String refStage;

    @Column(name = "file_group_id", length = 10)
    private String fileGroupId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 10)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 10)
    private String updatedBy;
}

