package com.cmms11.inventoryTx;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

/**
 * Inventory closing 집계 내역을 조회/저장하는 JPA 레포지토리.
 */
public interface InventoryClosingRepository extends JpaRepository<InventoryClosing, InventoryClosingId> {

    Optional<InventoryClosing> findByIdCompanyIdAndIdYyyymmAndIdStorageIdAndIdInventoryId(
        String companyId,
        String yyyymm,
        String storageId,
        String inventoryId
    );
    
    /**
     * 특정 창고의 모든 재고 마감 조회
     */
    List<InventoryClosing> findByIdCompanyIdAndIdYyyymmAndIdStorageId(
        String companyId,
        String yyyymm,
        String storageId
    );
    
    /**
     * 특정 월 이전의 가장 최근 마감 조회
     */
    @Query("SELECT c FROM InventoryClosing c " +
           "WHERE c.id.companyId = :companyId " +
           "AND c.id.storageId = :storageId " +
           "AND c.id.inventoryId = :inventoryId " +
           "AND c.id.yyyymm < :beforeYyyymm " +
           "ORDER BY c.id.yyyymm DESC")
    List<InventoryClosing> findLatestBeforeMonth(
        @Param("companyId") String companyId,
        @Param("storageId") String storageId,
        @Param("inventoryId") String inventoryId,
        @Param("beforeYyyymm") String beforeYyyymm
    );
}
