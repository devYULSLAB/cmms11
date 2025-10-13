package com.cmms11.workorder;

import com.cmms11.approval.ApprovalRequest;
import com.cmms11.approval.ApprovalResponse;
import com.cmms11.approval.ApprovalService;
import com.cmms11.common.error.NotFoundException;
import com.cmms11.common.seq.AutoNumberService;
import com.cmms11.security.MemberUserDetailsService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final String MODULE_CODE = "W";

    private final WorkOrderRepository repository;
    private final AutoNumberService autoNumberService;
    private final WorkOrderItemRepository itemRepository;

    @Autowired
    private ApprovalService approvalService;

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
        applyRequest(entity, request);  // 요청 데이터를 엔티티에 적용
        
        // ⭐ 신규 생성 시 초기 상태 설정
        // entity.setStage("PLN") or entity.setStage("ACT")
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
        entity.setUpdatedBy(currentMemberId());
        WorkOrder saved = repository.save(entity);
        List<WorkOrderItem> items = synchronizeItems(
            entity.getId().getCompanyId(),
            orderId,
            request.items()
        );
        return WorkOrderResponse.from(saved, items);
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

    private String currentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }
        String name = authentication.getName();
        return name != null ? name : "system";
    }

    /**
     * 계획 결재 요청
     */
    public ApprovalResponse submitPlanApproval(String workOrderId) {
        WorkOrder order = getExisting(workOrderId);
        
        // 상태 검증
        if (!"PLN".equals(order.getStage()) || !"DRAFT".equals(order.getStatus())) {
            throw new IllegalStateException("계획 작성 상태에서만 결재 요청 가능합니다. 현재 단계/상태: " + order.getStage() + "/" + order.getStatus());
        }
        
        // 결재 본문 생성
        String content = buildPlanApprovalContent(order);
        
        // Approval 생성
        ApprovalRequest request = new ApprovalRequest(
            null,
            "작업지시 계획 결재: " + order.getName(),
            "DRAFT",
            "WORK",
            workOrderId,
            "PLN",  // refStage
            content,
            order.getFileGroupId(),
            new ArrayList<>()
        );
        
        ApprovalResponse approval = approvalService.create(request);
        
        // 상태 변경 및 approvalId 저장
        order.setApprovalId(approval.approvalId());
        order.setStatus("SUBMT");
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(currentMemberId());
        repository.save(order);
        
        return approval;
    }

    /**
     * 실적 결재 요청
     */
    public ApprovalResponse submitActualApproval(String workOrderId) {
        WorkOrder order = getExisting(workOrderId);
        
        // 상태 검증
        if (!"ACT".equals(order.getStage()) || !"DRAFT".equals(order.getStatus())) {
            throw new IllegalStateException("실적 작성 상태에서만 결재 요청 가능합니다. 현재 상태: " + order.getStatus());
        }
        
        // 실적 필수값 검증
        if (order.getActualDate() == null) {
            throw new IllegalStateException("실적 정보를 모두 입력해주세요.");
        }
        
        // 결재 본문 생성
        String content = buildActualApprovalContent(order);
        
        // Approval 생성
        ApprovalRequest request = new ApprovalRequest(
            null,
            "작업지시 실적 결재: " + order.getName(),
            "DRAFT",
            "WORK",
            workOrderId,
            "ACT",  // refStage ⭐ 수정: "ACTUAL" → "ACT"
            content,
            order.getFileGroupId(),
            new ArrayList<>()
        );
        
        ApprovalResponse approval = approvalService.create(request);
        
        // 상태 변경 및 approvalId 저장 (기존 계획 approvalId 덮어씀)
        order.setApprovalId(approval.approvalId());
        order.setStatus("SUBMT");
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(currentMemberId());
        repository.save(order);
        
        return approval;
    }

    /**
     * 계획 결재 승인 콜백
     */
    public void onPlanApprovalApprove(String workOrderId) {
        WorkOrder order = getExisting(workOrderId);
        order.setStatus("APPRV");
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(currentMemberId());
        repository.save(order);
    }

    /**
     * 계획 자체 확정 (결재 없이 DRAFT → CMPLT)
     */
    public void onPlanApprovalComplete(String workOrderId) {
        WorkOrder order = getExisting(workOrderId);
        if (!"PLN".equals(order.getStage()) || !"DRAFT".equals(order.getStatus())) {
            throw new IllegalStateException("작성 중인 계획만 확정 가능합니다.");
        }
        order.setStatus("CMPLT");
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(currentMemberId());
        repository.save(order);
    }

    /**
     * 계획 결재 반려 콜백
     */
    public void onPlanApprovalReject(String workOrderId) {
        WorkOrder order = getExisting(workOrderId);
        order.setStatus("REJCT");
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(currentMemberId());
        repository.save(order);
    }

    /**
     * 계획 결재 삭제(취소) 콜백 - 상신 취소 시 DRAFT로 복원
     */
    public void onPlanApprovalDelete(String workOrderId) {
        WorkOrder order = getExisting(workOrderId);
        order.setStatus("DRAFT");
        order.setApprovalId(null);
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(currentMemberId());
        repository.save(order);
    }

    /**
     * 실적 결재 승인 콜백
     */
    public void onActualApprovalApprove(String workOrderId) {
        WorkOrder order = getExisting(workOrderId);
        order.setStatus("APPRV");
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(currentMemberId());
        repository.save(order);
    }

    /**
     * 실적 자체 확정 (결재 없이 DRAFT → CMPLT)
     */
    public void onActualApprovalComplete(String workOrderId) {
        WorkOrder order = getExisting(workOrderId);
        if (!"ACT".equals(order.getStage()) || !"DRAFT".equals(order.getStatus())) {
            throw new IllegalStateException("작성 중인 실적만 확정 가능합니다.");
        }
        order.setStatus("CMPLT");
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(currentMemberId());
        repository.save(order);
    }

    /**
     * 실적 결재 반려 콜백
     */
    public void onActualApprovalReject(String workOrderId) {
        WorkOrder order = getExisting(workOrderId);
        order.setStatus("REJCT");
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(currentMemberId());
        repository.save(order);
    }

    /**
     * 실적 결재 삭제(취소) 콜백 - 상신 취소 시 DRAFT로 복원
     */
    public void onActualApprovalDelete(String workOrderId) {
        WorkOrder order = getExisting(workOrderId);
        order.setStatus("DRAFT");
        order.setApprovalId(null);
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(currentMemberId());
        repository.save(order);
    }

    /**
     * 계획 결재 본문 생성
     */
    private String buildPlanApprovalContent(WorkOrder order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>작업지시 계획 결재 요청</h3>");
        sb.append("<table style='border-collapse:collapse; width:100%;'>");
        sb.append("<tr><th style='border:1px solid #ddd; padding:8px; background:#f5f5f5;'>항목</th>");
        sb.append("<th style='border:1px solid #ddd; padding:8px; background:#f5f5f5;'>내용</th></tr>");
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>작업지시 번호</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(order.getId().getOrderId()).append("</td></tr>");
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>작업명</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(order.getName() != null ? order.getName() : "-").append("</td></tr>");
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>설비</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(order.getPlantId() != null ? order.getPlantId() : "-").append("</td></tr>");
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>담당자</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(order.getMemberId() != null ? order.getMemberId() : "-").append("</td></tr>");
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>계획일</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(order.getPlannedDate() != null ? order.getPlannedDate() : "-").append("</td></tr>");
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>계획비용</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(order.getPlannedCost() != null ? order.getPlannedCost() : "-").append("</td></tr>");
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>계획공수</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(order.getPlannedLabor() != null ? order.getPlannedLabor() : "-").append("</td></tr>");
        sb.append("</table>");
        return sb.toString();
    }

    /**
     * 실적 입력 단계 준비 (PLN+APPRV → ACT+DRAFT)
     */
    public void prepareActualStage(String workOrderId) {
        WorkOrder order = getExisting(workOrderId);
        
        // 상태 검증
        if (!"PLN".equals(order.getStage()) || !"APPRV".equals(order.getStatus())) {
            throw new IllegalStateException(
                "계획 결재가 완료되어야 실적을 입력할 수 있습니다. 현재 단계/상태: " + 
                order.getStage() + "/" + order.getStatus()
            );
        }
        
        // 상태 전환
        order.setStage("ACT");
        order.setStatus("DRAFT");
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(currentMemberId());
        repository.save(order);
    }

    /**
     * 실적 결재 본문 생성
     */
    private String buildActualApprovalContent(WorkOrder order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>작업지시 실적 결재 요청</h3>");
        sb.append("<table style='border-collapse:collapse; width:100%;'>");
        sb.append("<tr><th style='border:1px solid #ddd; padding:8px; background:#f5f5f5;'>항목</th>");
        sb.append("<th style='border:1px solid #ddd; padding:8px; background:#f5f5f5;'>내용</th></tr>");
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>작업지시 번호</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(order.getId().getOrderId()).append("</td></tr>");
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>작업명</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(order.getName() != null ? order.getName() : "-").append("</td></tr>");
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>실적일</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(order.getActualDate() != null ? order.getActualDate() : "-").append("</td></tr>");
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>실적비용</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(order.getActualCost() != null ? order.getActualCost() : "-").append("</td></tr>");
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>실적공수</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(order.getActualLabor() != null ? order.getActualLabor() : "-").append("</td></tr>");
        sb.append("</table>");
        return sb.toString();
    }
}
