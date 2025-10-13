package com.cmms11.workorder;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "work_order")
@Getter
@Setter
@NoArgsConstructor
public class WorkOrder {

    @EmbeddedId
    private WorkOrderId id;

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

    @Column(name = "planned_cost", precision = 18, scale = 2)
    private BigDecimal plannedCost;

    @Column(name = "planned_labor", precision = 18, scale = 2)
    private BigDecimal plannedLabor;

    @Column(name = "actual_date")
    private LocalDate actualDate;

    @Column(name = "actual_cost", precision = 18, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "actual_labor", precision = 18, scale = 2)
    private BigDecimal actualLabor;

    @Column(name = "status", length = 10)
    private String status;

    @Column(name = "stage", length = 10)
    private String stage;

    @Column(name = "ref_entity", length = 10)
    private String refEntity;

    @Column(name = "ref_id", length = 10)
    private String refId;

    @Column(name = "ref_stage", length = 10)
    private String refStage;

    @Column(name = "approval_id", length = 10)
    private String approvalId;

    @Column(name = "file_group_id", length = 10)
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

