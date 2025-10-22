package com.cmms11.workorder;

import com.cmms11.common.error.NotFoundException;
import com.cmms11.common.seq.AutoNumberService;
import com.cmms11.security.MemberUserDetailsService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    private static final String MODULE_CODE = "W";

    private final WorkOrderRepository repository;
    private final AutoNumberService autoNumberService;
    private final WorkOrderItemRepository itemRepository;

    public WorkOrderService(WorkOrderRepository repository, AutoNumberService autoNumberService, WorkOrderItemRepository itemRepository) {
        this.repository = repository;
        this.autoNumberService = autoNumberService;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public Page<WorkOrderResponse> list(String orderId, String plantId, String status, String stage, String plannedDateFrom, String plannedDateTo, Pageable pageable) {
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
            stage,
            fromDate, 
            toDate, 
            pageable
        );
        
        return page.map(WorkOrderResponse::from);
    }

    @Transactional(readOnly = true)
    public WorkOrderResponse get(String orderId) {
        WorkOrder workOrder = getExisting(orderId);
        List<WorkOrderItem> items = itemRepository.findByOrder(
            MemberUserDetailsService.DEFAULT_COMPANY,
            orderId
        );
        return WorkOrderResponse.from(workOrder, items);
    }

    public WorkOrderResponse create(WorkOrderRequest request) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        LocalDateTime now = LocalDateTime.now();
        String memberId = MemberUserDetailsService.getCurrentMemberId();

        String newId = resolveId(companyId, request.orderId(), request.plannedDate());
        WorkOrder entity = new WorkOrder();
        entity.setId(new WorkOrderId(companyId, newId));
        entity.setCreatedAt(now);
        entity.setCreatedBy(memberId);
        applyRequest(entity, request);  // 요청 데이터를 엔티티에 적용
        
        // ⭐ 신규 생성 시 초기 상태 설정
        // request에서 stage가 전달되면 우선 사용 (PLN 또는 ACT)
        // 전달되지 않으면 기본값 "PLN" 사용
        if (request.stage() != null && !request.stage().isBlank()) {
            entity.setStage(request.stage());
        } else {
            entity.setStage("PLN");
        }
        entity.setStatus("DRAFT");

        entity.setUpdatedAt(now);
        entity.setUpdatedBy(memberId);

        WorkOrder saved = repository.save(entity);
        List<WorkOrderItem> items = synchronizeItems(companyId, newId, request.items());
        return WorkOrderResponse.from(saved, items);
    }

    public WorkOrderResponse update(String orderId, WorkOrderRequest request) {
        WorkOrder entity = getExisting(orderId);
        applyRequest(entity, request);  // 요청 데이터를 엔티티에 적용
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(MemberUserDetailsService.getCurrentMemberId());
        WorkOrder saved = repository.save(entity);
        List<WorkOrderItem> items = synchronizeItems(
            entity.getId().getCompanyId(),
            orderId,
            request.items()
        );
        return WorkOrderResponse.from(saved, items);
    }

    public void delete(String orderId) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        itemRepository.deleteByOrder(companyId, orderId);
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
        
        // ⭐ status/stage는 사용자 입력으로 변경 불가 (워크플로우로만 변경)
        // entity.setStatus(request.status());
        // entity.setStage(request.stage());
        
        entity.setRefEntity(request.refEntity());
        entity.setRefId(request.refId());
        entity.setRefStage(request.refStage());
        entity.setFileGroupId(request.fileGroupId());
        entity.setNote(request.note());
    }

    @Transactional(readOnly = true)
    public List<WorkOrderItem> getItems(String orderId) {
        return itemRepository.findByOrder(MemberUserDetailsService.DEFAULT_COMPANY, orderId);
    }

    private List<WorkOrderItem> synchronizeItems(
        String companyId,
        String orderId,
        List<WorkOrderItemRequest> items
    ) {
        itemRepository.deleteByOrder(companyId, orderId);
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<WorkOrderItem> entities = IntStream
            .range(0, items.size())
            .mapToObj(index -> toItemEntity(companyId, orderId, index + 1, items.get(index)))
            .collect(Collectors.toList());
        return itemRepository.saveAll(entities);
    }

    private WorkOrderItem toItemEntity(
        String companyId,
        String orderId,
        int lineNo,
        WorkOrderItemRequest item
    ) {
        WorkOrderItem entity = new WorkOrderItem();
        entity.setId(new WorkOrderItemId(companyId, orderId, lineNo));
        entity.setName(item.name());
        entity.setMethod(item.method());
        entity.setResult(item.result());
        entity.setNote(item.note());
        return entity;
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

    /**
     * 결재 승인 콜백 (PLN/ACT 통합)
     */
    public void onApprovalApprove(String workOrderId, String stage) {
        WorkOrder order = getExisting(workOrderId);
        order.setStatus("APPRV");
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(MemberUserDetailsService.getCurrentMemberId());
        repository.save(order);
    }

    /**
     * 결재 반려 콜백 (PLN/ACT 통합)
     */
    public void onApprovalReject(String workOrderId, String stage) {
        WorkOrder order = getExisting(workOrderId);
        order.setStatus("REJCT");
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(MemberUserDetailsService.getCurrentMemberId());
        repository.save(order);
    }

    /**
     * 결재 삭제 콜백 (PLN/ACT 통합)
     */
    public void onApprovalDelete(String workOrderId, String stage) {
        WorkOrder order = getExisting(workOrderId);
        order.setStatus("DRAFT");
        order.setApprovalId(null);
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(MemberUserDetailsService.getCurrentMemberId());
        repository.save(order);
    }

    /**
     * 담당자 확정 (결재 없이 DRAFT → CMPLT)
     * PLN/ACT 구분 없이 DRAFT 상태만 확정 가능
     */
    public void onComplete(String orderId) {
        WorkOrder order = getExisting(orderId);
        
        if (!"DRAFT".equals(order.getStatus())) {
            throw new IllegalStateException("작성 중인 문서만 확정 가능합니다.");
        }
        
        order.setStatus("CMPLT");
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(MemberUserDetailsService.getCurrentMemberId());
        repository.save(order);
    }

    /**
     * @deprecated Use {@link #onApprovalApprove(String, String)} instead
     */
    @Deprecated
    public void onPlanApprovalApprove(String workOrderId) {
        onApprovalApprove(workOrderId, "PLN");
    }

    /**
     * @deprecated Use {@link #onApprovalReject(String, String)} instead
     */
    @Deprecated
    public void onPlanApprovalReject(String workOrderId) {
        onApprovalReject(workOrderId, "PLN");
    }

    /**
     * @deprecated Use {@link #onApprovalDelete(String, String)} instead
     */
    @Deprecated
    public void onPlanApprovalDelete(String workOrderId) {
        onApprovalDelete(workOrderId, "PLN");
    }

    /**
     * @deprecated Use {@link #onApprovalApprove(String, String)} instead
     */
    @Deprecated
    public void onActualApprovalApprove(String workOrderId) {
        onApprovalApprove(workOrderId, "ACT");
    }

    /**
     * @deprecated Use {@link #onApprovalReject(String, String)} instead
     */
    @Deprecated
    public void onActualApprovalReject(String workOrderId) {
        onApprovalReject(workOrderId, "ACT");
    }

    /**
     * @deprecated Use {@link #onApprovalDelete(String, String)} instead
     */
    @Deprecated
    public void onActualApprovalDelete(String workOrderId) {
        onApprovalDelete(workOrderId, "ACT");
    }

    public void prepareActualStage(String workOrderId) {
        WorkOrder order = getExisting(workOrderId);

        if (!"PLN".equals(order.getStage()) || !"APPRV".equals(order.getStatus())) {
            throw new IllegalStateException("계획 결재가 완료되어야 실적을 입력할 수 있습니다.");
        }

        order.setStage("ACT");
        order.setStatus("DRAFT");
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(MemberUserDetailsService.getCurrentMemberId());
        repository.save(order);
    }

}
