package com.cmms11.workorder;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkOrderItemRepository extends JpaRepository<WorkOrderItem, WorkOrderItemId> {
    @Query("select i from WorkOrderItem i where i.id.companyId = :companyId and i.id.orderId = :orderId order by i.id.lineNo asc")
    List<WorkOrderItem> findByOrder(@Param("companyId") String companyId, @Param("orderId") String orderId);

    @Modifying
    @Query("delete from WorkOrderItem i where i.id.companyId = :companyId and i.id.orderId = :orderId")
    void deleteByOrder(@Param("companyId") String companyId, @Param("orderId") String orderId);
}

