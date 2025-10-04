package com.cmms11.web;

import com.cmms11.inventoryTx.InventoryTxService;
import com.cmms11.inventoryTx.InventoryClosingService;
import com.cmms11.inventoryTx.InventoryLedgerService;
import com.cmms11.inventoryTx.InventoryTxRequest;
import com.cmms11.inventoryTx.InventoryTxResponse;
import com.cmms11.inventoryTx.InventoryClosingRequest;
import com.cmms11.inventoryTx.InventoryClosingResponse;
import com.cmms11.inventoryTx.InventoryLedgerResponse;
import com.cmms11.inventoryTx.InventoryStockResponse;
import com.cmms11.domain.storage.StorageService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * 이름: InventoryTxController
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 재고거래 웹 화면 및 API 엔드포인트를 제공하는 컨트롤러.
 */
@Controller
public class InventoryTxController {

    private final InventoryTxService inventoryTxService;
    private final InventoryClosingService inventoryClosingService;
    private final InventoryLedgerService inventoryLedgerService;
    private final StorageService storageService;

    public InventoryTxController(
            InventoryTxService inventoryTxService,
            InventoryClosingService inventoryClosingService,
            InventoryLedgerService inventoryLedgerService,
            StorageService storageService) {
        this.inventoryTxService = inventoryTxService;
        this.inventoryClosingService = inventoryClosingService;
        this.inventoryLedgerService = inventoryLedgerService;
        this.storageService = storageService;
    }

    // 웹 컨트롤러 화면 제공
    @GetMapping("/inventoryTx/transaction")
    public String transactionForm(Model model) {
        model.addAttribute("txTypes", List.of("IN", "OUT", "MOVE", "ADJ"));
        addReferenceData(model);
        return "inventoryTx/transaction";
    }

    @GetMapping("/inventoryTx/closing")
    public String closingForm(@RequestParam(required = false) String yyyymm,
                              @RequestParam(required = false) String storageId,
                              @RequestParam(required = false) String inventoryId,
                              Model model) {
        // 조회 파라미터가 있으면 마감 데이터 조회
        if (yyyymm != null && !yyyymm.isEmpty() && storageId != null && !storageId.isEmpty()) {
            try {
                String closingMonth = yyyymm.replace("-", "");
                List<InventoryClosingResponse> closingList = inventoryClosingService.getClosingHistory(closingMonth, storageId, inventoryId);
                model.addAttribute("closingList", closingList);
                model.addAttribute("yyyymm", yyyymm);
                model.addAttribute("storageId", storageId);
                model.addAttribute("inventoryId", inventoryId);
            } catch (Exception e) {
                model.addAttribute("errorMessage", "마감 데이터 조회 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
        return "inventoryTx/closing";
    }
    
    @PostMapping("/inventoryTx/closing")
    public String processClosing(@RequestParam String yyyymm,
                                 @RequestParam String storageId,
                                 @RequestParam(required = false) String inventoryId,
                                 RedirectAttributes redirectAttributes) {
        try {
            // YYYY-MM을 YYYY-MM-01 형식의 LocalDate로 변환
            LocalDate closingDate = LocalDate.parse(yyyymm + "-01");
            
            // 마감 요청 생성 (계산 필드는 null, 서버에서 자동 계산)
            InventoryClosingRequest request = new InventoryClosingRequest(
                closingDate,
                storageId,
                inventoryId != null && !inventoryId.isEmpty() ? inventoryId : null,
                null, null, null, null, null, null, null, null, null, null, null, null
            );
            
            inventoryClosingService.processMonthlyClosing(request);
            redirectAttributes.addFlashAttribute("successMessage", "마감이 완료되었습니다.");
            redirectAttributes.addAttribute("yyyymm", yyyymm);
            redirectAttributes.addAttribute("storageId", storageId);
            if (inventoryId != null && !inventoryId.isEmpty()) {
                redirectAttributes.addAttribute("inventoryId", inventoryId);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "마감 처리 중 오류가 발생했습니다: " + e.getMessage());
            redirectAttributes.addAttribute("yyyymm", yyyymm);
            redirectAttributes.addAttribute("storageId", storageId);
            if (inventoryId != null && !inventoryId.isEmpty()) {
                redirectAttributes.addAttribute("inventoryId", inventoryId);
            }
        }
        return "redirect:/inventoryTx/closing";
    }

    @GetMapping("/inventoryTx/ledger")
    public String ledgerForm(@RequestParam(required = false) String fromYm,
                             @RequestParam(required = false) String toYm,
                             @RequestParam(required = false) String storageId,
                             @RequestParam(required = false) String inventoryId,
                             Model model) {
        // 조회 파라미터가 있으면 원장 데이터 조회
        if (fromYm != null && !fromYm.isEmpty() && toYm != null && !toYm.isEmpty()) {
            try {
                LocalDate fromDate = LocalDate.parse(fromYm + "-01");
                LocalDate toDate = LocalDate.parse(toYm + "-01").withDayOfMonth(
                    LocalDate.parse(toYm + "-01").lengthOfMonth());
                
                String cleanStorageId = (storageId != null && !storageId.isEmpty()) ? storageId : null;
                String cleanInventoryId = (inventoryId != null && !inventoryId.isEmpty()) ? inventoryId : null;
                
                InventoryLedgerService.LedgerSearchRequest searchRequest = 
                    new InventoryLedgerService.LedgerSearchRequest(
                        cleanStorageId, cleanInventoryId, fromDate, toDate);
                
                List<InventoryLedgerResponse> ledgerList = inventoryLedgerService.getLedger(searchRequest);
                
                model.addAttribute("ledgerList", ledgerList);
                model.addAttribute("fromYm", fromYm);
                model.addAttribute("toYm", toYm);
                model.addAttribute("storageId", storageId);
                model.addAttribute("inventoryId", inventoryId);
            } catch (Exception e) {
                model.addAttribute("errorMessage", "원장 조회 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
        return "inventoryTx/ledger";
    }

    // 거래 처리 API
    @PostMapping("/api/inventoryTx/transaction")
    @ResponseBody
    public ResponseEntity<InventoryTxResponse> processTransaction(@RequestBody InventoryTxRequest request) {
        try {
            InventoryTxResponse response = inventoryTxService.processTransaction(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 월별 마감 처리 API
    @PostMapping("/api/inventoryTx/closing")
    @ResponseBody
    public ResponseEntity<InventoryClosingResponse> processClosing(@RequestBody InventoryClosingRequest request) {
        try {
            InventoryClosingResponse response = inventoryClosingService.processMonthlyClosing(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 원장 조회 API
    @GetMapping("/api/inventoryTx/ledger")
    @ResponseBody
    public ResponseEntity<List<InventoryLedgerResponse>> getLedger(
            @RequestParam String companyId,
            @RequestParam(required = false) String storageId,
            @RequestParam(required = false) String inventoryId,
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        try {
            // 빈 문자열을 null로 변환
            String cleanStorageId = (storageId != null && !storageId.isEmpty()) ? storageId : null;
            String cleanInventoryId = (inventoryId != null && !inventoryId.isEmpty()) ? inventoryId : null;
            
            InventoryLedgerService.LedgerSearchRequest searchRequest = new InventoryLedgerService.LedgerSearchRequest(
                    cleanStorageId, cleanInventoryId, LocalDate.parse(fromDate), LocalDate.parse(toDate));
            List<InventoryLedgerResponse> ledger = inventoryLedgerService.getLedger(searchRequest);
            return ResponseEntity.ok(ledger);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 재고 조회 API
    @GetMapping("/api/inventoryTx/stock")
    @ResponseBody
    public ResponseEntity<InventoryStockResponse> getStock(
            @RequestParam String companyId,
            @RequestParam String storageId,
            @RequestParam String inventoryId) {
        try {
            InventoryStockResponse stock = inventoryTxService.getCurrentStock(companyId, inventoryId, storageId);
            return ResponseEntity.ok(stock);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 마감 조회 API
    @GetMapping("/api/inventoryTx/closing")
    @ResponseBody
    public ResponseEntity<List<InventoryClosingResponse>> getClosing(
            @RequestParam String closingMonth,
            @RequestParam String storageId,
            @RequestParam(required = false) String inventoryId) {
        try {
            List<InventoryClosingResponse> closing = inventoryClosingService.getClosingHistory(closingMonth, storageId, inventoryId);
            return ResponseEntity.ok(closing);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    private void addReferenceData(Model model) {
        // 창고 목록
        try {
            model.addAttribute("storages", storageService.list(null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("storages", List.of());
        }
    }
}
