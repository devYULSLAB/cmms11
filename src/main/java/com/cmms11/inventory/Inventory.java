package com.cmms11.inventory;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
public class Inventory {

    @EmbeddedId
    private InventoryId id;

    @Column(length = 100)
    private String name;

    @Column(name = "asset_id", length = 5)
    private String assetId;

    @Column(name = "dept_id", length = 5)
    private String deptId;

    @Column(name = "maker_name", length = 100)
    private String makerName;

    @Column(length = 100)
    private String spec;

    @Column(length = 100)
    private String model;

    @Column(length = 100)
    private String serial;

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

