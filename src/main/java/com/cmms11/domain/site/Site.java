package com.cmms11.domain.site;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "site")
@Getter
@Setter
@NoArgsConstructor
public class Site {

    @EmbeddedId
    private SiteId id;

    @Column(length = 100)
    private String name;

    @Column(length = 30)
    private String phone;

    @Column(length = 200)
    private String address;

    @Column(length = 500)
    private String note;

    @Column(name = "delete_mark", length = 1)
    private String deleteMark = "N";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 10)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 10)
    private String updatedBy;
}

