package com.cmms11.plant;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "plant")
@Getter
@Setter
@NoArgsConstructor
public class Plant {
    @EmbeddedId
    private PlantId id;

    @NotBlank
    @Column(length = 100)
    private String name;

    @Column(name = "asset_id", length = 5)
    private String assetId;

    @Column(name = "site_id", length = 5)
    private String siteId;

    @Column(name = "dept_id", length = 5)
    private String deptId;

    @Column(name = "func_id", length = 5)
    private String funcId;

    @Column(name = "maker_name", length = 100)
    private String makerName;

    @Column(length = 100)
    private String spec;

    @Column(length = 100)
    private String model;

    @Column(length = 100)
    private String serial;

    @Column(name = "install_date")
    private LocalDate installDate;

    @Column(name = "depre_id", length = 5)
    private String depreId;

    @Column(name = "depre_period")
    private Integer deprePeriod;

    @Column(name = "purchase_cost", precision = 18, scale = 2)
    private BigDecimal purchaseCost;

    @Column(name = "residual_value", precision = 18, scale = 2)
    private BigDecimal residualValue;

    @Column(name = "inspection_yn", length = 1)
    private String inspectionYn;

    @Column(name = "psm_yn", length = 1)
    private String psmYn;

    @Column(name = "workpermit_yn", length = 1)
    private String workpermitYn;

    @Column(name = "inspection_interval")
    private Integer inspectionInterval;

    @Column(name = "last_inspection")
    private LocalDate lastInspection;

    @Column(name = "next_inspection")
    private LocalDate nextInspection;

    @Column(name = "file_group_id", length = 10)
    private String fileGroupId;

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
