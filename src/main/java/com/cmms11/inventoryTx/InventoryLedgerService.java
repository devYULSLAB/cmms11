package com.cmms11.inventoryTx;

import com.cmms11.security.MemberUserDetailsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 이름: InventoryLedgerService
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 재고원장 비즈니스 로직을 처리하는 서비스.
 */
@Service
@Transactional(readOnly = true)
public class InventoryLedgerService {

    private final InventoryHistoryRepository historyRepository;

    public InventoryLedgerService(InventoryHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    /**
     * 원장 조회
     */
    public List<InventoryLedgerResponse> getLedger(LedgerSearchRequest request) {
        String companyId = MemberUserDetailsService.getCurrentUserCompanyId();
        
        // 1. 거래이력 조회
        List<InventoryHistory> histories = getTransactionHistories(companyId, request);
        
        // 2. 원장 데이터 생성
        return createLedgerData(histories, request);
    }

    /**
     * 원장 페이징 조회
     */
    public Page<InventoryLedgerResponse> getLedgerPage(LedgerSearchRequest request, Pageable pageable) {
        String companyId = MemberUserDetailsService.getCurrentUserCompanyId();
        
        // 1. 거래이력 페이징 조회
        Page<InventoryHistory> historyPage = getTransactionHistoriesPage(companyId, request, pageable);
        
        // 2. 원장 데이터 생성
        List<InventoryLedgerResponse> ledgerData = createLedgerData(historyPage.getContent(), request);
        
        // 3. 페이징 응답 생성
        return new PageImpl<>(ledgerData, pageable, historyPage.getTotalElements());
    }

    /**
     * 원장 요약 조회
     */
    public LedgerSummary getLedgerSummary(String companyId, String storageId, String inventoryId, LocalDate fromDate, LocalDate toDate) {
        // 1. 입고 수량 및 금액 집계
        BigDecimal totalInQty = calculateTotalInboundQty(companyId, storageId, inventoryId, fromDate, toDate);
        BigDecimal totalInAmount = calculateTotalInboundAmount(companyId, storageId, inventoryId, fromDate, toDate);
        
        // 2. 출고 수량 및 금액 집계
        BigDecimal totalOutQty = calculateTotalOutboundQty(companyId, storageId, inventoryId, fromDate, toDate);
        BigDecimal totalOutAmount = calculateTotalOutboundAmount(companyId, storageId, inventoryId, fromDate, toDate);
        
        // 3. 이동 수량 및 금액 집계
        BigDecimal totalMoveQty = calculateTotalMoveQty(companyId, storageId, inventoryId, fromDate, toDate);
        BigDecimal totalMoveAmount = calculateTotalMoveAmount(companyId, storageId, inventoryId, fromDate, toDate);
        
        // 4. 조정 수량 및 금액 집계
        BigDecimal totalAdjQty = calculateTotalAdjustmentQty(companyId, storageId, inventoryId, fromDate, toDate);
        BigDecimal totalAdjAmount = calculateTotalAdjustmentAmount(companyId, storageId, inventoryId, fromDate, toDate);
        
        // 5. 기초 및 기말 수량 계산
        BigDecimal beginQty = calculateBeginQty(companyId, storageId, inventoryId, fromDate);
        BigDecimal endQty = beginQty.add(totalInQty).subtract(totalOutQty).add(totalMoveQty).add(totalAdjQty);
        
        // 6. 기초 및 기말 금액 계산
        BigDecimal beginAmount = calculateBeginAmount(companyId, storageId, inventoryId, fromDate);
        BigDecimal endAmount = beginAmount.add(totalInAmount).subtract(totalOutAmount).add(totalMoveAmount).add(totalAdjAmount);
        
        return new LedgerSummary(
                beginQty, beginAmount,
                totalInQty, totalInAmount,
                totalOutQty, totalOutAmount,
                totalMoveQty, totalMoveAmount,
                totalAdjQty, totalAdjAmount,
                endQty, endAmount
        );
    }

    /**
     * 거래이력 조회
     */
    private List<InventoryHistory> getTransactionHistories(String companyId, LedgerSearchRequest request) {
        if (request.storageId() != null && request.inventoryId() != null) {
            return historyRepository.findByIdCompanyIdAndInventoryIdAndStorageIdAndTxDateBetween(
                    companyId, request.inventoryId(), request.storageId(), request.fromDate(), request.toDate(), Pageable.unpaged()).getContent();
        } else if (request.inventoryId() != null) {
            return historyRepository.findByIdCompanyIdAndInventoryIdAndTxDateBetween(
                    companyId, request.inventoryId(), request.fromDate(), request.toDate(), Pageable.unpaged()).getContent();
        } else if (request.storageId() != null) {
            return historyRepository.findByIdCompanyIdAndStorageIdAndTxDateBetween(
                    companyId, request.storageId(), request.fromDate(), request.toDate(), Pageable.unpaged()).getContent();
        } else {
            return historyRepository.findByIdCompanyIdAndTxDateBetween(
                    companyId, request.fromDate(), request.toDate(), Pageable.unpaged()).getContent();
        }
    }

    /**
     * 거래이력 페이징 조회
     */
    private Page<InventoryHistory> getTransactionHistoriesPage(String companyId, LedgerSearchRequest request, Pageable pageable) {
        if (request.storageId() != null && request.inventoryId() != null) {
            return historyRepository.findByIdCompanyIdAndInventoryIdAndStorageIdAndTxDateBetween(
                    companyId, request.inventoryId(), request.storageId(), request.fromDate(), request.toDate(), pageable);
        } else if (request.inventoryId() != null) {
            return historyRepository.findByIdCompanyIdAndInventoryIdAndTxDateBetween(
                    companyId, request.inventoryId(), request.fromDate(), request.toDate(), pageable);
        } else if (request.storageId() != null) {
            return historyRepository.findByIdCompanyIdAndStorageIdAndTxDateBetween(
                    companyId, request.storageId(), request.fromDate(), request.toDate(), pageable);
        } else {
            return historyRepository.findByIdCompanyIdAndTxDateBetween(
                    companyId, request.fromDate(), request.toDate(), pageable);
        }
    }

    /**
     * 원장 데이터 생성
     */
    private List<InventoryLedgerResponse> createLedgerData(List<InventoryHistory> histories, LedgerSearchRequest request) {
        List<InventoryLedgerResponse> ledgerData = new ArrayList<>();
        BigDecimal runningQty = BigDecimal.ZERO;
        BigDecimal runningAmount = BigDecimal.ZERO;
        
        for (InventoryHistory history : histories) {
            // 기초 수량 및 금액
            BigDecimal beginQty = runningQty;
            BigDecimal beginAmount = runningAmount;
            
            // 거래 수량 및 금액
            BigDecimal inQty = history.getInQty() != null ? history.getInQty() : BigDecimal.ZERO;
            BigDecimal inAmount = history.getInQty() != null && history.getAmount() != null ? history.getAmount() : BigDecimal.ZERO;
            BigDecimal outQty = history.getOutQty() != null ? history.getOutQty() : BigDecimal.ZERO;
            BigDecimal outAmount = history.getOutQty() != null && history.getAmount() != null ? history.getAmount() : BigDecimal.ZERO;
            BigDecimal moveQty = BigDecimal.ZERO;
            BigDecimal moveAmount = BigDecimal.ZERO;
            BigDecimal adjQty = BigDecimal.ZERO;
            BigDecimal adjAmount = BigDecimal.ZERO;
            
            // 거래유형별 처리
            switch (history.getTxType()) {
                case "MOVE" -> {
                    moveQty = inQty.subtract(outQty);
                    moveAmount = history.getAmount() != null ? history.getAmount() : BigDecimal.ZERO;
                }
                case "ADJ" -> {
                    adjQty = inQty.subtract(outQty);
                    adjAmount = history.getAmount() != null ? history.getAmount() : BigDecimal.ZERO;
                }
            }
            
            // 기말 수량 및 금액 계산
            runningQty = runningQty.add(inQty).subtract(outQty).add(moveQty).add(adjQty);
            runningAmount = runningAmount.add(inAmount).subtract(outAmount).add(moveAmount).add(adjAmount);
            
            // 원장 데이터 생성
            InventoryLedgerResponse ledger = new InventoryLedgerResponse(
                    history.getStorageId(),
                    history.getInventoryId(),
                    history.getTxDate().toString(),
                    history.getTxType(),
                    history.getRefNo(),
                    beginQty,
                    beginAmount,
                    inQty,
                    inAmount,
                    outQty,
                    outAmount,
                    moveQty,
                    moveAmount,
                    adjQty,
                    adjAmount,
                    runningQty,
                    runningAmount
            );
            
            ledgerData.add(ledger);
        }
        
        return ledgerData;
    }

    /**
     * 총 입고 수량 계산
     */
    private BigDecimal calculateTotalInboundQty(String companyId, String storageId, String inventoryId, LocalDate fromDate, LocalDate toDate) {
        BigDecimal result = historyRepository.sumInQtyByCompanyIdAndInventoryIdAndStorageIdAndTxDateBetween(
                companyId, inventoryId, storageId, fromDate, toDate);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 총 출고 수량 계산
     */
    private BigDecimal calculateTotalOutboundQty(String companyId, String storageId, String inventoryId, LocalDate fromDate, LocalDate toDate) {
        BigDecimal result = historyRepository.sumOutQtyByCompanyIdAndInventoryIdAndStorageIdAndTxDateBetween(
                companyId, inventoryId, storageId, fromDate, toDate);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 총 이동 수량 계산
     */
    private BigDecimal calculateTotalMoveQty(String companyId, String storageId, String inventoryId, LocalDate fromDate, LocalDate toDate) {
        BigDecimal result = historyRepository.sumQtyByCompanyIdAndInventoryIdAndStorageIdAndTxTypeAndTxDateBetween(
                companyId, inventoryId, storageId, "MOVE", fromDate, toDate);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 총 조정 수량 계산
     */
    private BigDecimal calculateTotalAdjustmentQty(String companyId, String storageId, String inventoryId, LocalDate fromDate, LocalDate toDate) {
        BigDecimal result = historyRepository.sumQtyByCompanyIdAndInventoryIdAndStorageIdAndTxTypeAndTxDateBetween(
                companyId, inventoryId, storageId, "ADJ", fromDate, toDate);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 총 입고 금액 계산
     */
    private BigDecimal calculateTotalInboundAmount(String companyId, String storageId, String inventoryId, LocalDate fromDate, LocalDate toDate) {
        BigDecimal result = historyRepository.sumInAmountByCompanyIdAndInventoryIdAndStorageIdAndTxDateBetween(
                companyId, inventoryId, storageId, fromDate, toDate);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 총 출고 금액 계산
     */
    private BigDecimal calculateTotalOutboundAmount(String companyId, String storageId, String inventoryId, LocalDate fromDate, LocalDate toDate) {
        BigDecimal result = historyRepository.sumOutAmountByCompanyIdAndInventoryIdAndStorageIdAndTxDateBetween(
                companyId, inventoryId, storageId, fromDate, toDate);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 총 이동 금액 계산
     */
    private BigDecimal calculateTotalMoveAmount(String companyId, String storageId, String inventoryId, LocalDate fromDate, LocalDate toDate) {
        BigDecimal result = historyRepository.sumAmountByCompanyIdAndInventoryIdAndStorageIdAndTxTypeAndTxDateBetween(
                companyId, inventoryId, storageId, "MOVE", fromDate, toDate);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 총 조정 금액 계산
     */
    private BigDecimal calculateTotalAdjustmentAmount(String companyId, String storageId, String inventoryId, LocalDate fromDate, LocalDate toDate) {
        BigDecimal result = historyRepository.sumAmountByCompanyIdAndInventoryIdAndStorageIdAndTxTypeAndTxDateBetween(
                companyId, inventoryId, storageId, "ADJ", fromDate, toDate);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 기초 수량 계산
     */
    private BigDecimal calculateBeginQty(String companyId, String storageId, String inventoryId, LocalDate fromDate) {
        // TODO: 기초 수량 계산 로직 구현
        return BigDecimal.ZERO;
    }

    /**
     * 기초 금액 계산
     */
    private BigDecimal calculateBeginAmount(String companyId, String storageId, String inventoryId, LocalDate fromDate) {
        // TODO: 기초 금액 계산 로직 구현
        return BigDecimal.ZERO;
    }

    /**
     * 원장 검색 요청 DTO
     */
    public record LedgerSearchRequest(
            String storageId,
            String inventoryId,
            LocalDate fromDate,
            LocalDate toDate
    ) {}

    /**
     * 원장 요약 DTO
     */
    public record LedgerSummary(
            BigDecimal beginQty, BigDecimal beginAmount,
            BigDecimal totalInQty, BigDecimal totalInAmount,
            BigDecimal totalOutQty, BigDecimal totalOutAmount,
            BigDecimal totalMoveQty, BigDecimal totalMoveAmount,
            BigDecimal totalAdjQty, BigDecimal totalAdjAmount,
            BigDecimal endQty, BigDecimal endAmount
    ) {}
}
