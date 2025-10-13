package com.cmms11.inventoryTx;

import com.cmms11.common.seq.AutoNumberService;
import com.cmms11.security.MemberUserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 이름: InventoryTxService
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 재고거래 비즈니스 로직을 처리하는 서비스.
 */
@Service
@Transactional
public class InventoryTxService {

    private final InventoryHistoryRepository historyRepository;
    private final InventoryStockRepository stockRepository;
    private final AutoNumberService autoNumberService;

    public InventoryTxService(
            InventoryHistoryRepository historyRepository,
            InventoryStockRepository stockRepository,
            AutoNumberService autoNumberService) {
        this.historyRepository = historyRepository;
        this.stockRepository = stockRepository;
        this.autoNumberService = autoNumberService;
    }

    /**
     * 재고거래 처리 (입고, 출고, 이동, 조정)
     */
    public InventoryTxResponse processTransaction(InventoryTxRequest request) {
        // 1. 유효성 검증
        validateTransaction(request);
        
        // 2. 거래 이력 생성
        List<InventoryHistory> histories = createTransactionHistories(request);
        for (InventoryHistory history : histories) {
            historyRepository.save(history);
        }
        
        // 3. 재고 업데이트
        if ("MOVE".equals(request.txType())) {
            // 이동은 출발/도착 창고 모두 처리
            updateStockForMove(request);
        } else {
            updateStock(request);
        }
        
        // 4. 응답 생성 (첫 번째 이력 기준)
        return createTransactionResponse(histories.get(0));
    }

    /**
     * 입고 처리
     */
    public InventoryTxResponse processInbound(InventoryTxRequest request) {
        if (!"IN".equals(request.txType())) {
            throw new IllegalArgumentException("입고 거래가 아닙니다.");
        }
        return processTransaction(request);
    }

    /**
     * 출고 처리
     */
    public InventoryTxResponse processOutbound(InventoryTxRequest request) {
        if (!"OUT".equals(request.txType())) {
            throw new IllegalArgumentException("출고 거래가 아닙니다.");
        }
        
        // 출고 가능 수량 확인
        validateStockAvailability(request);
        
        return processTransaction(request);
    }

    /**
     * 이동 처리
     */
    public InventoryTxResponse processMove(InventoryTxRequest request) {
        if (!"MOVE".equals(request.txType())) {
            throw new IllegalArgumentException("이동 거래가 아닙니다.");
        }
        
        // 이동 가능 수량 확인
        validateStockAvailability(request);
        
        return processTransaction(request);
    }

    /**
     * 조정 처리
     */
    public InventoryTxResponse processAdjustment(InventoryTxRequest request) {
        if (!"ADJ".equals(request.txType())) {
            throw new IllegalArgumentException("조정 거래가 아닙니다.");
        }
        return processTransaction(request);
    }

    /**
     * 현재 재고 조회
     */
    @Transactional(readOnly = true)
    public InventoryStockResponse getCurrentStock(String companyId, String inventoryId, String storageId) {
        Optional<InventoryStock> stock = stockRepository.findByIdCompanyIdAndIdInventoryIdAndIdStorageId(
                companyId, inventoryId, storageId);
        
        if (stock.isPresent()) {
            InventoryStock s = stock.get();
            return new InventoryStockResponse(
                    s.getId().getStorageId(),
                    s.getId().getInventoryId(),
                    s.getQty(),
                    s.getAmount(),
                    s.getUpdatedAt(),
                    s.getUpdatedBy()
            );
        } else {
            return new InventoryStockResponse(
                    storageId,
                    inventoryId,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    LocalDateTime.now(),
                    "SYSTEM"
            );
        }
    }

    /**
     * 창고별 재고 조회
     */
    @Transactional(readOnly = true)
    public List<InventoryStockResponse> getStockByStorage(String companyId, String storageId) {
        return stockRepository.findByIdCompanyIdAndIdStorageId(companyId, storageId, org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .map(stock -> new InventoryStockResponse(
                        stock.getId().getStorageId(),
                        stock.getId().getInventoryId(),
                        stock.getQty(),
                        stock.getAmount(),
                        stock.getUpdatedAt(),
                        stock.getUpdatedBy()
                ))
                .toList();
    }

    /**
     * 재고번호별 재고 조회
     */
    @Transactional(readOnly = true)
    public List<InventoryStockResponse> getStockByInventory(String companyId, String inventoryId) {
        return stockRepository.findByIdCompanyIdAndIdInventoryId(companyId, inventoryId, org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .map(stock -> new InventoryStockResponse(
                        stock.getId().getStorageId(),
                        stock.getId().getInventoryId(),
                        stock.getQty(),
                        stock.getAmount(),
                        stock.getUpdatedAt(),
                        stock.getUpdatedBy()
                ))
                .toList();
    }

    /**
     * 거래 유효성 검증
     */
    private void validateTransaction(InventoryTxRequest request) {
        if (request.inventoryId() == null || request.inventoryId().trim().isEmpty()) {
            throw new IllegalArgumentException("재고번호는 필수입니다.");
        }
        if (request.storageId() == null || request.storageId().trim().isEmpty()) {
            throw new IllegalArgumentException("창고번호는 필수입니다.");
        }
        if (request.txType() == null || request.txType().trim().isEmpty()) {
            throw new IllegalArgumentException("거래유형은 필수입니다.");
        }
        if (request.txDate() == null) {
            throw new IllegalArgumentException("거래일자는 필수입니다.");
        }
        
        // 거래유형별 유효성 검증
        switch (request.txType()) {
            case "IN" -> {
                if (request.inQty() == null || request.inQty().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("입고 수량은 0보다 커야 합니다.");
                }
                if (request.outQty() != null && request.outQty().compareTo(BigDecimal.ZERO) != 0) {
                    throw new IllegalArgumentException("입고 거래에서는 출고 수량이 0이어야 합니다.");
                }
            }
            case "OUT" -> {
                if (request.outQty() == null || request.outQty().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("출고 수량은 0보다 커야 합니다.");
                }
                if (request.inQty() != null && request.inQty().compareTo(BigDecimal.ZERO) != 0) {
                    throw new IllegalArgumentException("출고 거래에서는 입고 수량이 0이어야 합니다.");
                }
            }
            case "MOVE" -> {
                if (request.moveQty() == null || request.moveQty().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("이동 수량은 0보다 커야 합니다.");
                }
                if (request.srcStorageId() == null || request.srcStorageId().isEmpty()) {
                    throw new IllegalArgumentException("출발 창고는 필수입니다.");
                }
                if (request.dstStorageId() == null || request.dstStorageId().isEmpty()) {
                    throw new IllegalArgumentException("도착 창고는 필수입니다.");
                }
            }
            case "ADJ" -> {
                if (request.adjQty() == null || request.adjQty().compareTo(BigDecimal.ZERO) == 0) {
                    throw new IllegalArgumentException("조정 수량은 필수이며 0이 아니어야 합니다.");
                }
            }
            default -> throw new IllegalArgumentException("지원하지 않는 거래유형입니다: " + request.txType());
        }
    }

    /**
     * 재고 가용성 검증
     */
    private void validateStockAvailability(InventoryTxRequest request) {
        String companyId = MemberUserDetailsService.getCurrentUserCompanyId();
        
        BigDecimal currentQty = stockRepository.findQtyByIdCompanyIdAndIdInventoryIdAndIdStorageId(
                companyId, request.inventoryId(), request.storageId());
        
        if (currentQty == null) {
            currentQty = BigDecimal.ZERO;
        }
        
        BigDecimal requiredQty = request.outQty() != null ? request.outQty() : BigDecimal.ZERO;
        
        if (currentQty.compareTo(requiredQty) < 0) {
            throw new IllegalArgumentException(
                    String.format("재고 부족: 현재 수량 %s, 요청 수량 %s", currentQty, requiredQty));
        }
    }

    /**
     * 거래 이력 생성 (MOVE는 2건, 나머지는 1건)
     */
    private List<InventoryHistory> createTransactionHistories(InventoryTxRequest request) {
        String companyId = MemberUserDetailsService.getCurrentUserCompanyId();
        List<InventoryHistory> histories = new java.util.ArrayList<>();
        
        if ("MOVE".equals(request.txType())) {
            // 이동 거래는 2건의 이력 생성 (출고 + 입고)
            String moveRefNo = autoNumberService.generateTxId(companyId, "MV", request.txDate());
            
            // 1. 출발 창고 출고
            InventoryHistory outHistory = new InventoryHistory();
            outHistory.setId(new InventoryHistoryId(companyId, 
                autoNumberService.generateTxId(companyId, "H", request.txDate())));
            outHistory.setInventoryId(request.inventoryId());
            outHistory.setStorageId(request.srcStorageId());
            outHistory.setTxType("MOVE");
            outHistory.setRefNo(moveRefNo);
            outHistory.setRefLine(1);
            outHistory.setTxDate(request.txDate());
            outHistory.setOutQty(request.moveQty());
            outHistory.setNote("이동출고 → " + request.dstStorageId() + 
                (request.note() != null && !request.note().isEmpty() ? " (" + request.note() + ")" : ""));
            setAuditFields(outHistory);
            histories.add(outHistory);
            
            // 2. 도착 창고 입고
            InventoryHistory inHistory = new InventoryHistory();
            inHistory.setId(new InventoryHistoryId(companyId, 
                autoNumberService.generateTxId(companyId, "H", request.txDate())));
            inHistory.setInventoryId(request.inventoryId());
            inHistory.setStorageId(request.dstStorageId());
            inHistory.setTxType("MOVE");
            inHistory.setRefNo(moveRefNo);
            inHistory.setRefLine(2);
            inHistory.setTxDate(request.txDate());
            inHistory.setInQty(request.moveQty());
            inHistory.setNote("이동입고 ← " + request.srcStorageId() + 
                (request.note() != null && !request.note().isEmpty() ? " (" + request.note() + ")" : ""));
            setAuditFields(inHistory);
            histories.add(inHistory);
            
        } else {
            // 일반 거래는 1건
            histories.add(createSingleHistory(request));
        }
        
        return histories;
    }
    
    /**
     * 단일 거래 이력 생성
     */
    private InventoryHistory createSingleHistory(InventoryTxRequest request) {
        String companyId = MemberUserDetailsService.getCurrentUserCompanyId();
        String historyId = autoNumberService.generateTxId(companyId, "H", request.txDate());
        
        InventoryHistory history = new InventoryHistory();
        history.setId(new InventoryHistoryId(companyId, historyId));
        history.setInventoryId(request.inventoryId());
        history.setStorageId(request.storageId());
        history.setTxType(request.txType());
        history.setRefNo(request.refNo());
        history.setRefLine(request.refLine());
        history.setTxDate(request.txDate());
        history.setInQty(request.inQty());
        history.setOutQty(request.outQty());
        history.setUnitCost(request.unitCost());
        history.setAmount(request.amount());
        history.setNote(request.note());
        setAuditFields(history);
        
        return history;
    }
    
    /**
     * 감사 필드 설정
     */
    private void setAuditFields(InventoryHistory history) {
        LocalDateTime now = LocalDateTime.now();
        history.setCreatedAt(now);
        history.setCreatedBy("SYSTEM"); // TODO: 실제 사용자 ID로 변경
        history.setUpdatedAt(now);
        history.setUpdatedBy("SYSTEM");
    }

    /**
     * 재고 업데이트
     */
    private void updateStock(InventoryTxRequest request) {
        String companyId = MemberUserDetailsService.getCurrentUserCompanyId();
        String storageId = request.storageId();
        String inventoryId = request.inventoryId();
        
        // 현재 재고 조회
        Optional<InventoryStock> existingStock = stockRepository.findByIdCompanyIdAndIdInventoryIdAndIdStorageId(
                companyId, inventoryId, storageId);
        
        InventoryStock stock = existingStock.orElse(new InventoryStock());
        if (!existingStock.isPresent()) {
            stock.setId(new InventoryStockId(companyId, storageId, inventoryId));
            stock.setQty(BigDecimal.ZERO);
            stock.setAmount(BigDecimal.ZERO);
        }
        
        // 수량 업데이트
        BigDecimal newQty = stock.getQty();
        if (request.inQty() != null) {
            newQty = newQty.add(request.inQty());
        }
        if (request.outQty() != null) {
            newQty = newQty.subtract(request.outQty());
        }
        // 조정(ADJ) 수량 처리
        if (request.adjQty() != null) {
            newQty = newQty.add(request.adjQty());
        }
        stock.setQty(newQty);
        
        // 금액 업데이트
        BigDecimal newAmount = stock.getAmount();
        if (request.amount() != null) {
            newAmount = newAmount.add(request.amount());
        }
        // 조정(ADJ) 금액 처리
        if (request.adjAmount() != null) {
            newAmount = newAmount.add(request.adjAmount());
        }
        stock.setAmount(newAmount);
        
        stock.setUpdatedAt(LocalDateTime.now());
        stock.setUpdatedBy("SYSTEM"); // TODO: 실제 사용자 ID로 변경
        
        stockRepository.save(stock);
    }

    /**
     * 이동 거래 재고 업데이트 (출발/도착 창고 모두 처리)
     */
    private void updateStockForMove(InventoryTxRequest request) {
        String companyId = MemberUserDetailsService.getCurrentUserCompanyId();
        String inventoryId = request.inventoryId();
        BigDecimal moveQty = request.moveQty();
        
        if (moveQty == null || moveQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("이동 수량은 0보다 커야 합니다.");
        }
        
        if (request.srcStorageId() == null || request.dstStorageId() == null) {
            throw new IllegalArgumentException("출발 창고와 도착 창고는 필수입니다.");
        }
        
        if (request.srcStorageId().equals(request.dstStorageId())) {
            throw new IllegalArgumentException("출발 창고와 도착 창고가 같을 수 없습니다.");
        }
        
        // 1. 출발 창고에서 차감
        Optional<InventoryStock> srcStock = stockRepository.findByIdCompanyIdAndIdInventoryIdAndIdStorageId(
                companyId, inventoryId, request.srcStorageId());
        
        if (!srcStock.isPresent()) {
            throw new IllegalArgumentException("출발 창고에 재고가 없습니다.");
        }
        
        InventoryStock src = srcStock.get();
        if (src.getQty().compareTo(moveQty) < 0) {
            throw new IllegalArgumentException(
                    String.format("재고 부족: 현재 수량 %s, 이동 수량 %s", src.getQty(), moveQty));
        }
        
        src.setQty(src.getQty().subtract(moveQty));
        src.setUpdatedAt(LocalDateTime.now());
        src.setUpdatedBy("SYSTEM");
        stockRepository.save(src);
        
        // 2. 도착 창고에 추가
        Optional<InventoryStock> dstStock = stockRepository.findByIdCompanyIdAndIdInventoryIdAndIdStorageId(
                companyId, inventoryId, request.dstStorageId());
        
        InventoryStock dst = dstStock.orElse(new InventoryStock());
        if (!dstStock.isPresent()) {
            dst.setId(new InventoryStockId(companyId, request.dstStorageId(), inventoryId));
            dst.setQty(BigDecimal.ZERO);
            dst.setAmount(BigDecimal.ZERO);
        }
        
        dst.setQty(dst.getQty().add(moveQty));
        dst.setUpdatedAt(LocalDateTime.now());
        dst.setUpdatedBy("SYSTEM");
        stockRepository.save(dst);
    }

    /**
     * 거래 응답 생성
     */
    private InventoryTxResponse createTransactionResponse(InventoryHistory history) {
        return new InventoryTxResponse(
                history.getId().getHistoryId(),
                history.getInventoryId(),
                history.getStorageId(),
                history.getTxType(),
                history.getRefNo(),
                history.getRefLine(),
                history.getTxDate(),
                history.getInQty(),
                history.getOutQty(),
                history.getUnitCost(),
                history.getAmount(),
                history.getNote(),
                history.getCreatedAt(),
                history.getCreatedBy(),
                history.getUpdatedAt(),
                history.getUpdatedBy()
        );
    }
}
