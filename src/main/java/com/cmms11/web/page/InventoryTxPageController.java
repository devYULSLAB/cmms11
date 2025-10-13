package com.cmms11.web.page;

import com.cmms11.domain.storage.StorageService;
import com.cmms11.inventoryTx.InventoryClosingResponse;
import com.cmms11.inventoryTx.InventoryClosingService;
import com.cmms11.inventoryTx.InventoryLedgerResponse;
import com.cmms11.inventoryTx.InventoryLedgerService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 이름: InventoryTxPageController
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 재고거래 관리 페이지 컨트롤러 (HTML 반환)
 */
@Controller
public class InventoryTxPageController {

    private final InventoryClosingService inventoryClosingService;
    private final InventoryLedgerService inventoryLedgerService;
    private final StorageService storageService;

    public InventoryTxPageController(
        InventoryClosingService inventoryClosingService,
        InventoryLedgerService inventoryLedgerService,
        StorageService storageService
    ) {
        this.inventoryClosingService = inventoryClosingService;
        this.inventoryLedgerService = inventoryLedgerService;
        this.storageService = storageService;
    }

    /**
     * 재고 거래 입력 페이지
     */
    @GetMapping("/inventoryTx/transaction")
    public String transaction(
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Model model
    ) {
        model.addAttribute("txTypes", List.of("IN", "OUT", "MOVE", "ADJ"));
        addReferenceData(model);
        
        return _fragment ? "inventoryTx/transaction :: content" : "inventoryTx/transaction";
    }

    /**
     * 재고 마감 페이지
     */
    @GetMapping("/inventoryTx/closing")
    public String closing(
        @RequestParam(required = false) String yyyymm,
        @RequestParam(required = false) String storageId,
        @RequestParam(required = false) String inventoryId,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Model model
    ) {
        // 조회 파라미터가 있으면 마감 데이터 조회
        if (yyyymm != null && !yyyymm.isEmpty() && storageId != null && !storageId.isEmpty()) {
            try {
                String closingMonth = yyyymm.replace("-", "");
                List<InventoryClosingResponse> closingList = inventoryClosingService.getClosingHistory(
                    closingMonth, storageId, inventoryId
                );
                model.addAttribute("closingList", closingList);
                model.addAttribute("yyyymm", yyyymm);
                model.addAttribute("storageId", storageId);
                model.addAttribute("inventoryId", inventoryId);
            } catch (Exception e) {
                model.addAttribute("errorMessage", "마감 데이터 조회 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
        
        addReferenceData(model);
        
        return _fragment ? "inventoryTx/closing :: content" : "inventoryTx/closing";
    }

    /**
     * 재고 원장 페이지
     */
    @GetMapping("/inventoryTx/ledger")
    public String ledger(
        @RequestParam(required = false) String fromDate,
        @RequestParam(required = false) String toDate,
        @RequestParam(required = false) String storageId,
        @RequestParam(required = false) String inventoryId,
        @RequestParam(required = false, defaultValue = "false") boolean _fragment,
        Model model
    ) {
        // 조회 파라미터가 있으면 원장 데이터 조회
        if (fromDate != null && toDate != null && storageId != null && inventoryId != null) {
            try {
                LocalDate from = LocalDate.parse(fromDate);
                LocalDate to = LocalDate.parse(toDate);
                InventoryLedgerService.LedgerSearchRequest searchRequest = 
                    new InventoryLedgerService.LedgerSearchRequest(storageId, inventoryId, from, to);
                List<InventoryLedgerResponse> ledger = inventoryLedgerService.getLedger(searchRequest);
                model.addAttribute("ledger", ledger);
                model.addAttribute("fromDate", fromDate);
                model.addAttribute("toDate", toDate);
                model.addAttribute("storageId", storageId);
                model.addAttribute("inventoryId", inventoryId);
            } catch (Exception e) {
                model.addAttribute("errorMessage", "원장 조회 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
        
        addReferenceData(model);
        
        return _fragment ? "inventoryTx/ledger :: content" : "inventoryTx/ledger";
    }

    /**
     * 참조 데이터 추가
     */
    private void addReferenceData(Model model) {
        try {
            model.addAttribute("storages", storageService.list(null, org.springframework.data.domain.Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("storages", List.of());
        }
    }
}

