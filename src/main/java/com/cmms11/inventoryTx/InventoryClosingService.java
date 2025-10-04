package com.cmms11.inventoryTx;

import com.cmms11.common.seq.AutoNumberService;
import com.cmms11.security.MemberUserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 이름: InventoryClosingService
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 재고마감 비즈니스 로직을 처리하는 서비스.
 */
@Service
@Transactional
public class InventoryClosingService {

    private final InventoryHistoryRepository historyRepository;
    private final InventoryClosingRepository closingRepository;
    private final AutoNumberService autoNumberService;

    public InventoryClosingService(
            InventoryHistoryRepository historyRepository,
            InventoryClosingRepository closingRepository,
            AutoNumberService autoNumberService) {
        this.historyRepository = historyRepository;
        this.closingRepository = closingRepository;
        this.autoNumberService = autoNumberService;
    }

    /**
     * 월별 마감 처리
     * 1. 최초 마감 → 전월 기말 0으로 시작
     * 2. 전월 마감 누락 → 마지막 마감 이후 누락 월들을 모두 마감 처리
     * 3. 전월 마감 있음 → 당월만 마감 처리
     */
    public InventoryClosingResponse processMonthlyClosing(InventoryClosingRequest request) {
        String companyId = MemberUserDetailsService.getCurrentUserCompanyId();
        String targetYyyymm = request.closingDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        
        // 1. 이미 마감되었는지 확인
        validateClosingNotExists(companyId, request.storageId(), request.inventoryId(), request.closingDate());
        
        // 2. 전월 마감 확인 및 누락 월 처리
        fillMissingMonths(companyId, request.storageId(), request.inventoryId(), request.closingDate());
        
        // 3. 당월 마감 요약 계산
        ClosingSummary summary = calculateClosingSummary(
            companyId, request.storageId(), request.inventoryId(), request.closingDate());
        
        // 4. 마감 데이터 생성 및 저장
        InventoryClosing closing = new InventoryClosing();
        closing.setId(new InventoryClosingId(companyId, targetYyyymm, request.storageId(), request.inventoryId()));
        closing.setBeginQty(summary.beginQty());
        closing.setBeginAmount(summary.beginAmount());
        closing.setInQty(summary.inQty());
        closing.setInAmount(summary.inAmount());
        closing.setOutQty(summary.outQty());
        closing.setOutAmount(summary.outAmount());
        closing.setMoveQty(summary.moveQty());
        closing.setMoveAmount(summary.moveAmount());
        closing.setAdjQty(summary.adjQty());
        closing.setAdjAmount(summary.adjAmount());
        closing.setEndQty(summary.endQty());
        closing.setEndAmount(summary.endAmount());
        closing.setStatus("CLOSED");
        closing.setClosedAt(LocalDateTime.now());
        closing.setClosedBy("SYSTEM"); // TODO: 실제 사용자 ID로 변경
        
        closingRepository.save(closing);
        
        // 5. 응답 생성
        return createClosingResponse(closing);
    }
    
    /**
     * 누락된 월 마감 자동 처리
     */
    private void fillMissingMonths(String companyId, String storageId, String inventoryId, LocalDate targetDate) {
        LocalDate prevMonth = targetDate.minusMonths(1);
        String prevYyyymm = prevMonth.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        
        // 전월 마감 확인
        Optional<InventoryClosing> prevClosing = closingRepository
            .findByIdCompanyIdAndIdYyyymmAndIdStorageIdAndIdInventoryId(
                companyId, prevYyyymm, storageId, inventoryId);
        
        if (prevClosing.isPresent()) {
            // 전월 마감 있음 → 정상, 추가 처리 불필요
            return;
        }
        
        // 가장 최근 마감 조회
        Optional<InventoryClosing> latestClosing = findLatestClosing(companyId, storageId, inventoryId, prevYyyymm);
        
        LocalDate startMonth;
        if (latestClosing.isEmpty()) {
            // 최초 마감 → 시스템에 첫 거래가 발생한 월부터 시작
            Optional<LocalDate> firstTxDate = historyRepository
                .findFirstTxDateByCompanyIdAndStorageIdAndInventoryId(companyId, storageId, inventoryId);
            
            if (firstTxDate.isEmpty()) {
                // 거래 이력이 없으면 전월부터 시작 (기초=0)
                startMonth = prevMonth;
            } else {
                // 첫 거래 월부터 시작
                startMonth = firstTxDate.get().withDayOfMonth(1);
            }
        } else {
            // 마지막 마감 다음 월부터 시작
            String lastYyyymm = latestClosing.get().getId().getYyyymm();
            int year = Integer.parseInt(lastYyyymm.substring(0, 4));
            int month = Integer.parseInt(lastYyyymm.substring(4, 6));
            startMonth = LocalDate.of(year, month, 1).plusMonths(1);
        }
        
        // 누락된 월들을 순차적으로 마감 처리
        LocalDate currentMonth = startMonth;
        while (currentMonth.isBefore(targetDate)) {
            processMonthClosing(companyId, storageId, inventoryId, currentMonth);
            currentMonth = currentMonth.plusMonths(1);
        }
    }
    
    /**
     * 특정 월 마감 처리 (내부용)
     */
    private void processMonthClosing(String companyId, String storageId, String inventoryId, LocalDate closingMonth) {
        String yyyymm = closingMonth.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        
        // 이미 마감되었는지 확인
        Optional<InventoryClosing> existing = closingRepository
            .findByIdCompanyIdAndIdYyyymmAndIdStorageIdAndIdInventoryId(
                companyId, yyyymm, storageId, inventoryId);
        
        if (existing.isPresent()) {
            return; // 이미 마감됨
        }
        
        // 마감 요약 계산
        ClosingSummary summary = calculateClosingSummary(companyId, storageId, inventoryId, closingMonth);
        
        // 마감 데이터 생성 및 저장
        InventoryClosing closing = new InventoryClosing();
        closing.setId(new InventoryClosingId(companyId, yyyymm, storageId, inventoryId));
        closing.setBeginQty(summary.beginQty());
        closing.setBeginAmount(summary.beginAmount());
        closing.setInQty(summary.inQty());
        closing.setInAmount(summary.inAmount());
        closing.setOutQty(summary.outQty());
        closing.setOutAmount(summary.outAmount());
        closing.setMoveQty(summary.moveQty());
        closing.setMoveAmount(summary.moveAmount());
        closing.setAdjQty(summary.adjQty());
        closing.setAdjAmount(summary.adjAmount());
        closing.setEndQty(summary.endQty());
        closing.setEndAmount(summary.endAmount());
        closing.setStatus("CLOSED");
        closing.setClosedAt(LocalDateTime.now());
        closing.setClosedBy("SYSTEM");
        
        closingRepository.save(closing);
    }
    
    /**
     * 가장 최근 마감 조회 (특정 월 이전)
     */
    private Optional<InventoryClosing> findLatestClosing(String companyId, String storageId, String inventoryId, String beforeYyyymm) {
        List<InventoryClosing> list = closingRepository.findLatestBeforeMonth(
            companyId, storageId, inventoryId, beforeYyyymm);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * 마감 데이터 존재 여부 확인
     */
    private void validateClosingNotExists(String companyId, String storageId, String inventoryId, LocalDate closingDate) {
        // TODO: 마감 데이터 존재 여부 확인 로직 구현
        // 이미 마감된 데이터가 있는지 확인
    }


    /**
     * 마감 응답 생성
     */
    private InventoryClosingResponse createClosingResponse(InventoryClosing closing) {
        return new InventoryClosingResponse(
                closing.getId().getYyyymm(),
                closing.getId().getStorageId(),
                closing.getId().getInventoryId(),
                closing.getBeginQty(),
                closing.getBeginAmount(),
                closing.getInQty(),
                closing.getInAmount(),
                closing.getOutQty(),
                closing.getOutAmount(),
                closing.getMoveQty(),
                closing.getMoveAmount(),
                closing.getAdjQty(),
                closing.getAdjAmount(),
                closing.getEndQty(),
                closing.getEndAmount(),
                closing.getStatus(),
                closing.getClosedAt(),
                closing.getClosedBy()
        );
    }

    /**
     * 기초재고 계산
     * 1. 전월 마감 있음 → 전월 기말 사용
     * 2. 전월 마감 없음 → 0 (최초 마감 또는 누락, 별도 처리 필요)
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateBeginStock(String companyId, String storageId, String inventoryId, LocalDate beginDate) {
        LocalDate prevMonth = beginDate.minusMonths(1);
        String prevYyyymm = prevMonth.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        
        Optional<InventoryClosing> prevClosing = closingRepository
            .findByIdCompanyIdAndIdYyyymmAndIdStorageIdAndIdInventoryId(
                companyId, prevYyyymm, storageId, inventoryId);
        
        if (prevClosing.isPresent()) {
            // 전월 마감 있음 → 전월 기말 사용
            return prevClosing.get().getEndQty();
        }
        
        // 전월 마감 없음 → 0 (최초 마감 또는 마감 누락)
        return BigDecimal.ZERO;
    }
    
    /**
     * 기초금액 계산
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateBeginAmount(String companyId, String storageId, String inventoryId, LocalDate beginDate) {
        LocalDate prevMonth = beginDate.minusMonths(1);
        String prevYyyymm = prevMonth.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        
        Optional<InventoryClosing> prevClosing = closingRepository
            .findByIdCompanyIdAndIdYyyymmAndIdStorageIdAndIdInventoryId(
                companyId, prevYyyymm, storageId, inventoryId);
        
        if (prevClosing.isPresent()) {
            return prevClosing.get().getEndAmount();
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * 입고 수량 집계
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateInboundQty(String companyId, String storageId, String inventoryId, LocalDate fromDate, LocalDate toDate) {
        BigDecimal result = historyRepository.sumInQtyByCompanyIdAndInventoryIdAndStorageIdAndTxDateBetween(
                companyId, inventoryId, storageId, fromDate, toDate);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 출고 수량 집계
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateOutboundQty(String companyId, String storageId, String inventoryId, LocalDate fromDate, LocalDate toDate) {
        BigDecimal result = historyRepository.sumOutQtyByCompanyIdAndInventoryIdAndStorageIdAndTxDateBetween(
                companyId, inventoryId, storageId, fromDate, toDate);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 이동 수량 집계
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateMoveQty(String companyId, String storageId, String inventoryId, LocalDate fromDate, LocalDate toDate) {
        BigDecimal result = historyRepository.sumQtyByCompanyIdAndInventoryIdAndStorageIdAndTxTypeAndTxDateBetween(
                companyId, inventoryId, storageId, "MOVE", fromDate, toDate);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 조정 수량 집계
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateAdjustmentQty(String companyId, String storageId, String inventoryId, LocalDate fromDate, LocalDate toDate) {
        BigDecimal result = historyRepository.sumQtyByCompanyIdAndInventoryIdAndStorageIdAndTxTypeAndTxDateBetween(
                companyId, inventoryId, storageId, "ADJ", fromDate, toDate);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 입고 금액 집계
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateInboundAmount(String companyId, String storageId, String inventoryId, LocalDate fromDate, LocalDate toDate) {
        BigDecimal result = historyRepository.sumInAmountByCompanyIdAndInventoryIdAndStorageIdAndTxDateBetween(
                companyId, inventoryId, storageId, fromDate, toDate);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 출고 금액 집계
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateOutboundAmount(String companyId, String storageId, String inventoryId, LocalDate fromDate, LocalDate toDate) {
        BigDecimal result = historyRepository.sumOutAmountByCompanyIdAndInventoryIdAndStorageIdAndTxDateBetween(
                companyId, inventoryId, storageId, fromDate, toDate);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 이동 금액 집계
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateMoveAmount(String companyId, String storageId, String inventoryId, LocalDate fromDate, LocalDate toDate) {
        BigDecimal result = historyRepository.sumAmountByCompanyIdAndInventoryIdAndStorageIdAndTxTypeAndTxDateBetween(
                companyId, inventoryId, storageId, "MOVE", fromDate, toDate);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 조정 금액 집계
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateAdjustmentAmount(String companyId, String storageId, String inventoryId, LocalDate fromDate, LocalDate toDate) {
        BigDecimal result = historyRepository.sumAmountByCompanyIdAndInventoryIdAndStorageIdAndTxTypeAndTxDateBetween(
                companyId, inventoryId, storageId, "ADJ", fromDate, toDate);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 마감 요약 계산
     */
    @Transactional(readOnly = true)
    public ClosingSummary calculateClosingSummary(String companyId, String storageId, String inventoryId, LocalDate closingDate) {
        LocalDate beginDate = closingDate.withDayOfMonth(1);
        LocalDate endDate = closingDate.withDayOfMonth(closingDate.lengthOfMonth());
        
        BigDecimal beginQty = calculateBeginStock(companyId, storageId, inventoryId, beginDate);
        BigDecimal beginAmount = calculateBeginAmount(companyId, storageId, inventoryId, beginDate);
        
        BigDecimal inQty = calculateInboundQty(companyId, storageId, inventoryId, beginDate, endDate);
        BigDecimal inAmount = calculateInboundAmount(companyId, storageId, inventoryId, beginDate, endDate);
        
        BigDecimal outQty = calculateOutboundQty(companyId, storageId, inventoryId, beginDate, endDate);
        BigDecimal outAmount = calculateOutboundAmount(companyId, storageId, inventoryId, beginDate, endDate);
        
        BigDecimal moveQty = calculateMoveQty(companyId, storageId, inventoryId, beginDate, endDate);
        BigDecimal moveAmount = calculateMoveAmount(companyId, storageId, inventoryId, beginDate, endDate);
        
        BigDecimal adjQty = calculateAdjustmentQty(companyId, storageId, inventoryId, beginDate, endDate);
        BigDecimal adjAmount = calculateAdjustmentAmount(companyId, storageId, inventoryId, beginDate, endDate);
        
        // 기말 수량 및 금액 계산
        BigDecimal endQty = beginQty.add(inQty).subtract(outQty).add(moveQty).add(adjQty);
        BigDecimal endAmount = beginAmount.add(inAmount).subtract(outAmount).add(moveAmount).add(adjAmount);
        
        return new ClosingSummary(
                beginQty, beginAmount,
                inQty, inAmount,
                outQty, outAmount,
                moveQty, moveAmount,
                adjQty, adjAmount,
                endQty, endAmount
        );
    }

    /**
     * 마감 이력 조회
     */
    @Transactional(readOnly = true)
    public List<InventoryClosingResponse> getClosingHistory(String closingMonth, String storageId, String inventoryId) {
        String companyId = MemberUserDetailsService.getCurrentUserCompanyId();
        
        List<InventoryClosing> closingList;
        
        if (inventoryId != null && !inventoryId.isEmpty()) {
            // 특정 재고에 대한 마감 조회
            Optional<InventoryClosing> closing = closingRepository
                .findByIdCompanyIdAndIdYyyymmAndIdStorageIdAndIdInventoryId(
                    companyId, closingMonth, storageId, inventoryId);
            closingList = closing.map(List::of).orElse(List.of());
        } else {
            // 창고 내 모든 재고의 마감 조회
            closingList = closingRepository.findByIdCompanyIdAndIdYyyymmAndIdStorageId(
                companyId, closingMonth, storageId);
        }
        
        return closingList.stream()
            .map(this::createClosingResponse)
            .toList();
    }

    /**
     * 마감 상세 조회
     */
    @Transactional(readOnly = true)
    public Optional<InventoryClosingResponse> getClosingDetail(String companyId, String closingId) {
        // TODO: 마감 상세 조회 로직 구현
        return Optional.empty();
    }

    /**
     * 마감 요약 DTO
     */
    public record ClosingSummary(
            BigDecimal beginQty, BigDecimal beginAmount,
            BigDecimal inQty, BigDecimal inAmount,
            BigDecimal outQty, BigDecimal outAmount,
            BigDecimal moveQty, BigDecimal moveAmount,
            BigDecimal adjQty, BigDecimal adjAmount,
            BigDecimal endQty, BigDecimal endAmount
    ) {}
}
