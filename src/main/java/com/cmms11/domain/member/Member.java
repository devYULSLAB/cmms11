package com.cmms11.domain.member;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "member")
@Getter
@Setter
@NoArgsConstructor
public class Member {

    @EmbeddedId
    private MemberId id;

    @Column(length = 100)
    private String name;

    @Column(name = "dept_id", length = 5)
    private String deptId;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(length = 100)
    private String email;

    @Column(length = 100)
    private String phone;

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

    // 편의 메서드들
    public String getMemberId() {
        return id != null ? id.getMemberId() : null;
    }

    public String getCompanyId() {
        return id != null ? id.getCompanyId() : null;
    }
}
