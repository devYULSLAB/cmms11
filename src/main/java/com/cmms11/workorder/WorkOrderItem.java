package com.cmms11.workorder;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "work_order_item")
@Getter
@Setter
@NoArgsConstructor
public class WorkOrderItem {

    @EmbeddedId
    private WorkOrderItemId id;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "method", length = 100)
    private String method;

    @Column(name = "result", length = 100)
    private String result;

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

