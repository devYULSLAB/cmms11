package com.cmms11.workpermit;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "work_permit_item")
@Getter
@Setter
@NoArgsConstructor
public class WorkPermitItem {

    @EmbeddedId
    private WorkPermitItemId id;

    @Column(name = "name", length = 100)
    private String name;

    @Lob
    @Column(name = "signature", columnDefinition = "MEDIUMTEXT")
    private String signature; // base64 PNG (MEDIUMTEXT for large signatures)

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 10)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 10)
    private String updatedBy;
}

