package com.cmms11.web.api;

import com.cmms11.inventoryTx.InventoryClosingRequest;
import com.cmms11.inventoryTx.InventoryClosingResponse;
import com.cmms11.inventoryTx.InventoryClosingService;
import com.cmms11.inventoryTx.InventoryLedgerResponse;
import com.cmms11.inventoryTx.InventoryLedgerService;
import com.cmms11.inventoryTx.InventoryStockResponse;
import com.cmms11.inventoryTx.InventoryTxRequest;
import com.cmms11.inventoryTx.InventoryTxResponse;
import com.cmms11.inventoryTx.InventoryTxService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이름: InventoryTxApiController
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 재고거래 관리 API 컨트롤러 (JSON 반환)
 */
@RestController
@RequestMapping("/api/inventoryTx")
public class InventoryTxApiController {

    private final InventoryTxService inventoryTxService;
    private final InventoryClosingService inventoryClosingService;
    private final InventoryLedgerService inventoryLedgerService;

    public InventoryTxApiController(
        InventoryTxService inventoryTxService,
        InventoryClosingService inventoryClosingService,
        InventoryLedgerService inventoryLedgerService
    ) {
        this.inventoryTxService = inventoryTxService;
        this.inventoryClosingService = inventoryClosingService;
        this.inventoryLedgerService = inventoryLedgerService;
    }

    /**
     * 재고 거래 등록
     */
    @PostMapping("/transaction")
    public ResponseEntity<InventoryTxResponse> processTransaction(@Valid @RequestBody InventoryTxRequest request) {
        InventoryTxResponse response = inventoryTxService.processTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 재고 현황 조회 (창고별)
     */
    @GetMapping("/stock/by-storage")
    public ResponseEntity<List<InventoryStockResponse>> getStockByStorage(
        @RequestParam String companyId,
        @RequestParam String storageId
    ) {
        List<InventoryStockResponse> stocks = inventoryTxService.getStockByStorage(companyId, storageId);
        return ResponseEntity.ok(stocks);
    }

    /**
     * 재고 현황 조회 (품목별)
     */
    @GetMapping("/stock/by-inventory")
    public ResponseEntity<List<InventoryStockResponse>> getStockByInventory(
        @RequestParam String companyId,
        @RequestParam String inventoryId
    ) {
        List<InventoryStockResponse> stocks = inventoryTxService.getStockByInventory(companyId, inventoryId);
        return ResponseEntity.ok(stocks);
    }

    /**
     * 현재 재고 조회
     */
    @GetMapping("/stock/current")
    public ResponseEntity<InventoryStockResponse> getCurrentStock(
        @RequestParam String companyId,
        @RequestParam String inventoryId,
        @RequestParam String storageId
    ) {
        InventoryStockResponse stock = inventoryTxService.getCurrentStock(companyId, inventoryId, storageId);
        return ResponseEntity.ok(stock);
    }

    /**
     * 재고 마감 처리
     */
    @PostMapping("/closing")
    public ResponseEntity<InventoryClosingResponse> processClosing(@Valid @RequestBody InventoryClosingRequest request) {
        InventoryClosingResponse closing = inventoryClosingService.processMonthlyClosing(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(closing);
    }

    /**
     * 마감 이력 조회
     */
    @GetMapping("/closing/history")
    public ResponseEntity<List<InventoryClosingResponse>> getClosingHistory(
        @RequestParam String yyyymm,
        @RequestParam String storageId,
        @RequestParam(required = false) String inventoryId
    ) {
        String closingMonth = yyyymm.replace("-", "");
        List<InventoryClosingResponse> closings = inventoryClosingService.getClosingHistory(
            closingMonth, storageId, inventoryId
        );
        return ResponseEntity.ok(closings);
    }

    /**
     * 재고 원장 조회
     */
    @GetMapping("/ledger")
    public ResponseEntity<List<InventoryLedgerResponse>> getLedger(
        @RequestParam String storageId,
        @RequestParam String inventoryId,
        @RequestParam String fromDate,
        @RequestParam String toDate
    ) {
        LocalDate from = LocalDate.parse(fromDate);
        LocalDate to = LocalDate.parse(toDate);
        InventoryLedgerService.LedgerSearchRequest searchRequest = 
            new InventoryLedgerService.LedgerSearchRequest(storageId, inventoryId, from, to);
        List<InventoryLedgerResponse> ledger = inventoryLedgerService.getLedger(searchRequest);
        return ResponseEntity.ok(ledger);
    }
}

