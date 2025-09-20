package com.cmms11.inspection;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inspection")
@Getter
@Setter
@NoArgsConstructor
public class Inspection {

    @EmbeddedId
    private InspectionId id;

    @Column(length = 100)
    private String name;

    @Column(name = "plant_id", length = 10)
    private String plantId;

    @Column(name = "job_id", length = 5)
    private String jobId;

    @Column(name = "site_id", length = 5)
    private String siteId;

    @Column(name = "dept_id", length = 5)
    private String deptId;

    @Column(name = "member_id", length = 5)
    private String memberId;

    @Column(name = "planned_date")
    private LocalDate plannedDate;

    @Column(name = "actual_date")
    private LocalDate actualDate;

    @Column(name = "status", length = 10)
    private String status;

    @Column(name = "file_group_id", length = 100)
    private String fileGroupId;

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
}

