package com.cmms11.workpermit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkPermitItemRepository extends JpaRepository<WorkPermitItem, WorkPermitItemId> {
    @Query("select i from WorkPermitItem i where i.id.companyId = :companyId and i.id.permitId = :permitId order by i.id.lineNo asc")
    List<WorkPermitItem> findByPermit(@Param("companyId") String companyId, @Param("permitId") String permitId);

    @Modifying
    @Query("delete from WorkPermitItem i where i.id.companyId = :companyId and i.id.permitId = :permitId")
    void deleteByPermit(@Param("companyId") String companyId, @Param("permitId") String permitId);
}

