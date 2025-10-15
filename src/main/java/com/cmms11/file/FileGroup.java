package com.cmms11.file;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "file_group")
@Getter
@Setter
@NoArgsConstructor
public class FileGroup {

    @EmbeddedId
    private FileGroupId id;

    @Column(name = "ref_entity", length = 64)
    private String refEntity;

    @Column(name = "ref_id", length = 10)
    private String refId;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 10)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 10)
    private String updatedBy;

    @Column(name = "delete_mark", length = 1)
    private String deleteMark = "N";
}
