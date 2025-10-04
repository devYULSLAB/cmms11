package com.cmms11.inventoryTx;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 이름: InventoryHistoryRepository
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 재고거래이력 데이터 접근 계층.
 */
@Repository
public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, InventoryHistoryId> {

    /**
     * 회사별 재고거래이력 조회
     */
    Page<InventoryHistory> findByIdCompanyId(String companyId, Pageable pageable);

    /**
     * 회사별, 재고번호별 재고거래이력 조회
     */
    Page<InventoryHistory> findByIdCompanyIdAndInventoryId(String companyId, String inventoryId, Pageable pageable);

    /**
     * 회사별, 창고번호별 재고거래이력 조회
     */
    Page<InventoryHistory> findByIdCompanyIdAndStorageId(String companyId, String storageId, Pageable pageable);

    /**
     * 회사별, 재고번호별, 창고번호별 재고거래이력 조회
     */
    Page<InventoryHistory> findByIdCompanyIdAndInventoryIdAndStorageId(
            String companyId, String inventoryId, String storageId, Pageable pageable);

    /**
     * 회사별, 거래유형별 재고거래이력 조회
     */
    Page<InventoryHistory> findByIdCompanyIdAndTxType(String companyId, String txType, Pageable pageable);

    /**
     * 회사별, 기간별 재고거래이력 조회
     */
    Page<InventoryHistory> findByIdCompanyIdAndTxDateBetween(
            String companyId, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    /**
     * 회사별, 재고번호별, 기간별 재고거래이력 조회
     */
    Page<InventoryHistory> findByIdCompanyIdAndInventoryIdAndTxDateBetween(
            String companyId, String inventoryId, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    /**
     * 회사별, 창고번호별, 기간별 재고거래이력 조회
     */
    Page<InventoryHistory> findByIdCompanyIdAndStorageIdAndTxDateBetween(
            String companyId, String storageId, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    /**
     * 회사별, 재고번호별, 창고번호별, 기간별 재고거래이력 조회
     */
    Page<InventoryHistory> findByIdCompanyIdAndInventoryIdAndStorageIdAndTxDateBetween(
            String companyId, String inventoryId, String storageId, 
            LocalDate fromDate, LocalDate toDate, Pageable pageable);

    /**
     * 회사별, 참조번호별 재고거래이력 조회
     */
    List<InventoryHistory> findByIdCompanyIdAndRefNo(String companyId, String refNo);

    /**
     * 회사별, 재고번호별, 창고번호별, 거래유형별 재고거래이력 조회
     */
    List<InventoryHistory> findByIdCompanyIdAndInventoryIdAndStorageIdAndTxType(
            String companyId, String inventoryId, String storageId, String txType);

    /**
     * 회사별, 재고번호별, 창고번호별, 기간별 입고 수량 합계 조회
     */
    @Query("SELECT COALESCE(SUM(h.inQty), 0) FROM InventoryHistory h " +
           "WHERE h.id.companyId = :companyId " +
           "AND h.inventoryId = :inventoryId " +
           "AND h.storageId = :storageId " +
           "AND h.txDate BETWEEN :fromDate AND :toDate")
    BigDecimal sumInQtyByCompanyIdAndInventoryIdAndStorageIdAndTxDateBetween(
            @Param("companyId") String companyId,
            @Param("inventoryId") String inventoryId,
            @Param("storageId") String storageId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * 회사별, 재고번호별, 창고번호별, 기간별 출고 수량 합계 조회
     */
    @Query("SELECT COALESCE(SUM(h.outQty), 0) FROM InventoryHistory h " +
           "WHERE h.id.companyId = :companyId " +
           "AND h.inventoryId = :inventoryId " +
           "AND h.storageId = :storageId " +
           "AND h.txDate BETWEEN :fromDate AND :toDate")
    BigDecimal sumOutQtyByCompanyIdAndInventoryIdAndStorageIdAndTxDateBetween(
            @Param("companyId") String companyId,
            @Param("inventoryId") String inventoryId,
            @Param("storageId") String storageId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * 회사별, 재고번호별, 창고번호별, 기간별 입고 금액 합계 조회
     */
    @Query("SELECT COALESCE(SUM(h.amount), 0) FROM InventoryHistory h " +
           "WHERE h.id.companyId = :companyId " +
           "AND h.inventoryId = :inventoryId " +
           "AND h.storageId = :storageId " +
           "AND h.txDate BETWEEN :fromDate AND :toDate " +
           "AND h.inQty > 0")
    BigDecimal sumInAmountByCompanyIdAndInventoryIdAndStorageIdAndTxDateBetween(
            @Param("companyId") String companyId,
            @Param("inventoryId") String inventoryId,
            @Param("storageId") String storageId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * 회사별, 재고번호별, 창고번호별, 기간별 출고 금액 합계 조회
     */
    @Query("SELECT COALESCE(SUM(h.amount), 0) FROM InventoryHistory h " +
           "WHERE h.id.companyId = :companyId " +
           "AND h.inventoryId = :inventoryId " +
           "AND h.storageId = :storageId " +
           "AND h.txDate BETWEEN :fromDate AND :toDate " +
           "AND h.outQty > 0")
    BigDecimal sumOutAmountByCompanyIdAndInventoryIdAndStorageIdAndTxDateBetween(
            @Param("companyId") String companyId,
            @Param("inventoryId") String inventoryId,
            @Param("storageId") String storageId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * 회사별, 재고번호별, 창고번호별, 거래유형별, 기간별 수량 합계 조회
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN h.txType = 'IN' THEN h.inQty " +
           "WHEN h.txType = 'OUT' THEN -h.outQty " +
           "WHEN h.txType = 'MOVE' THEN h.inQty - h.outQty " +
           "WHEN h.txType = 'ADJ' THEN h.inQty - h.outQty " +
           "ELSE 0 END), 0) FROM InventoryHistory h " +
           "WHERE h.id.companyId = :companyId " +
           "AND h.inventoryId = :inventoryId " +
           "AND h.storageId = :storageId " +
           "AND h.txType = :txType " +
           "AND h.txDate BETWEEN :fromDate AND :toDate")
    BigDecimal sumQtyByCompanyIdAndInventoryIdAndStorageIdAndTxTypeAndTxDateBetween(
            @Param("companyId") String companyId,
            @Param("inventoryId") String inventoryId,
            @Param("storageId") String storageId,
            @Param("txType") String txType,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * 회사별, 재고번호별, 창고번호별, 거래유형별, 기간별 금액 합계 조회
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN h.txType = 'IN' THEN h.amount " +
           "WHEN h.txType = 'OUT' THEN -h.amount " +
           "WHEN h.txType = 'MOVE' THEN h.amount " +
           "WHEN h.txType = 'ADJ' THEN h.amount " +
           "ELSE 0 END), 0) FROM InventoryHistory h " +
           "WHERE h.id.companyId = :companyId " +
           "AND h.inventoryId = :inventoryId " +
           "AND h.storageId = :storageId " +
           "AND h.txType = :txType " +
           "AND h.txDate BETWEEN :fromDate AND :toDate")
    BigDecimal sumAmountByCompanyIdAndInventoryIdAndStorageIdAndTxTypeAndTxDateBetween(
            @Param("companyId") String companyId,
            @Param("inventoryId") String inventoryId,
            @Param("storageId") String storageId,
            @Param("txType") String txType,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
    
    /**
     * 특정 재고/창고의 최초 거래 날짜 조회
     */
    @Query("SELECT MIN(h.txDate) FROM InventoryHistory h " +
           "WHERE h.id.companyId = :companyId " +
           "AND h.storageId = :storageId " +
           "AND h.inventoryId = :inventoryId")
    Optional<LocalDate> findFirstTxDateByCompanyIdAndStorageIdAndInventoryId(
            @Param("companyId") String companyId,
            @Param("storageId") String storageId,
            @Param("inventoryId") String inventoryId);
}
