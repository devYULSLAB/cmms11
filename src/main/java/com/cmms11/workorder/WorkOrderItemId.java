package com.cmms11.workorder;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class WorkOrderItemId implements Serializable {
    @Column(name = "company_id", length = 5)
    private String companyId;

    @Column(name = "order_id", length = 10)
    private String orderId;

    @Column(name = "line_no")
    private Integer lineNo;
}

