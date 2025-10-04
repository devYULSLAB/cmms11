package com.cmms11.workorder;

import com.cmms11.common.error.NotFoundException;
import com.cmms11.common.seq.AutoNumberService;
import com.cmms11.security.MemberUserDetailsService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이름: WorkOrderService
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 작업지시 트랜잭션 CRUD 로직을 제공하는 서비스.
 */
@Service
@Transactional
public class WorkOrderService {

    private static final String MODULE_CODE = "O";

    private final WorkOrderRepository repository;
    private final AutoNumberService autoNumberService;
    private final WorkOrderItemRepository itemRepository;

    public WorkOrderService(WorkOrderRepository repository, AutoNumberService autoNumberService, WorkOrderItemRepository itemRepository) {
        this.repository = repository;
        this.autoNumberService = autoNumberService;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public Page<WorkOrderResponse> list(String orderId, String plantId, String status, String plannedDateFrom, String plannedDateTo, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        
        // 날짜 문자열을 LocalDate로 변환
        java.time.LocalDate fromDate = null;
        java.time.LocalDate toDate = null;
        
        try {
            if (plannedDateFrom != null && !plannedDateFrom.isBlank()) {
                fromDate = java.time.LocalDate.parse(plannedDateFrom);
            }
            if (plannedDateTo != null && !plannedDateTo.isBlank()) {
                toDate = java.time.LocalDate.parse(plannedDateTo);
            }
        } catch (java.time.format.DateTimeParseException e) {
            // 날짜 파싱 오류 시 무시
        }
        
        Page<WorkOrder> page = repository.findByFilters(
            companyId, 
            orderId, 
            plantId, 
            status, 
            fromDate, 
            toDate, 
            pageable
        );
        
        return page.map(WorkOrderResponse::from);
    }

    @Transactional(readOnly = true)
    public WorkOrderResponse get(String orderId) {
        return WorkOrderResponse.from(getExisting(orderId));
    }

    public WorkOrderResponse create(WorkOrderRequest request) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        LocalDateTime now = LocalDateTime.now();
        String memberId = currentMemberId();

        String newId = resolveId(companyId, request.orderId(), request.plannedDate());
        WorkOrder entity = new WorkOrder();
        entity.setId(new WorkOrderId(companyId, newId));
        entity.setCreatedAt(now);
        entity.setCreatedBy(memberId);
        applyRequest(entity, request);
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(memberId);

        WorkOrder saved = repository.save(entity);
        saveItems(saved.getId().getCompanyId(), saved.getId().getOrderId(), request);
        return WorkOrderResponse.from(saved);
    }

    public WorkOrderResponse update(String orderId, WorkOrderRequest request) {
        WorkOrder entity = getExisting(orderId);
        applyRequest(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentMemberId());
        WorkOrder saved = repository.save(entity);
        saveItems(saved.getId().getCompanyId(), saved.getId().getOrderId(), request);
        return WorkOrderResponse.from(saved);
    }

    public void delete(String orderId) {
        WorkOrder entity = getExisting(orderId);
        repository.delete(entity);
    }

    private WorkOrder getExisting(String orderId) {
        return repository
            .findByIdCompanyIdAndIdOrderId(MemberUserDetailsService.DEFAULT_COMPANY, orderId)
            .orElseThrow(() -> new NotFoundException("WorkOrder not found: " + orderId));
    }

    private void applyRequest(WorkOrder entity, WorkOrderRequest request) {
        entity.setName(request.name());
        entity.setPlantId(request.plantId());
        entity.setJobId(request.jobId());
        entity.setSiteId(request.siteId());
        entity.setDeptId(request.deptId());
        entity.setMemberId(request.memberId());
        entity.setPlannedDate(request.plannedDate());
        entity.setPlannedCost(request.plannedCost());
        entity.setPlannedLabor(request.plannedLabor());
        entity.setActualDate(request.actualDate());
        entity.setActualCost(request.actualCost());
        entity.setActualLabor(request.actualLabor());
        entity.setStatus(request.status());
        entity.setFileGroupId(request.fileGroupId());
        entity.setNote(request.note());
    }

    @Transactional(readOnly = true)
    public java.util.List<WorkOrderItem> getItems(String orderId) {
        return itemRepository.findByOrder(MemberUserDetailsService.DEFAULT_COMPANY, orderId);
    }

    private void saveItems(String companyId, String orderId, WorkOrderRequest request) {
        itemRepository.deleteByOrder(companyId, orderId);
        if (request.items() == null || request.items().isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        String memberId = currentMemberId();
        int line = 1;
        for (WorkOrderItemRequest r : request.items()) {
            if (r == null) continue;
            WorkOrderItem e = new WorkOrderItem();
            e.setId(new WorkOrderItemId(companyId, orderId, line++));
            e.setName(r.name());
            e.setMethod(r.method());
            e.setResult(r.result());
            e.setNote(r.note());
            e.setCreatedAt(now);
            e.setCreatedBy(memberId);
            e.setUpdatedAt(now);
            e.setUpdatedBy(memberId);
            itemRepository.save(e);
        }
    }

    private String resolveId(String companyId, String requestedId, LocalDate referenceDate) {
        if (requestedId != null && !requestedId.isBlank()) {
            String trimmed = requestedId.trim();
            repository
                .findByIdCompanyIdAndIdOrderId(companyId, trimmed)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("WorkOrder already exists: " + trimmed);
                });
            return trimmed;
        }
        LocalDate date = referenceDate != null ? referenceDate : LocalDate.now();
        return autoNumberService.generateTxId(companyId, MODULE_CODE, date);
    }

    private String currentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }
        String name = authentication.getName();
        return name != null ? name : "system";
    }
}
