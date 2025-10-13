package com.cmms11.inspection;

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
 * 이름: InspectionService
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 예방점검 트랜잭션 CRUD 로직을 담당하는 서비스.
 */
@Service
@Transactional
public class InspectionService {

    private static final String MODULE_CODE = "I";

    private final InspectionRepository repository;
    private final InspectionItemRepository itemRepository;
    private final AutoNumberService autoNumberService;
    
    @Autowired
    private ApprovalService approvalService;

    public InspectionService(
        InspectionRepository repository,
        InspectionItemRepository itemRepository,
        AutoNumberService autoNumberService
    ) {
        this.repository = repository;
        this.itemRepository = itemRepository;
        this.autoNumberService = autoNumberService;
    }

    @Transactional(readOnly = true)
    public Page<InspectionResponse> list(String inspectionId, String plantId, String name, String status, String stage, String plannedDateFrom, String plannedDateTo, Pageable pageable) {
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
        
        Page<Inspection> page = repository.findByFilters(
            companyId, 
            inspectionId, 
            plantId, 
            name, 
            status, 
            stage, 
            fromDate, 
            toDate, 
            pageable
        );
        
        return page.map(InspectionResponse::from);
    }

    @Transactional(readOnly = true)
    public InspectionResponse get(String inspectionId) {
        Inspection inspection = getExisting(inspectionId);
        List<InspectionItem> items = itemRepository
            .findByIdCompanyIdAndIdInspectionIdOrderByIdLineNo(
                MemberUserDetailsService.DEFAULT_COMPANY,
                inspectionId
            );
        return InspectionResponse.from(inspection, items);
    }

    public InspectionResponse create(InspectionRequest request) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        LocalDateTime now = LocalDateTime.now();
        String memberId = currentMemberId();

        String newId = resolveId(companyId, request.inspectionId(), request.plannedDate());
        Inspection entity = new Inspection();
        entity.setId(new InspectionId(companyId, newId));
        entity.setCreatedAt(now);
        entity.setCreatedBy(memberId);
        applyRequest(entity, request);
        
        // ⭐ 신규 생성 시 초기 상태 설정 (계획 자동 승인)
        // entity.setStage("PLN");
        // entity.setStatus("APPRV");
        
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(memberId);

        Inspection saved = repository.save(entity);
        List<InspectionItem> items = synchronizeItems(companyId, newId, request.items());
        return InspectionResponse.from(saved, items);
    }

    public InspectionResponse update(String inspectionId, InspectionRequest request) {
        Inspection entity = getExisting(inspectionId);
        applyRequest(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentMemberId());
        Inspection saved = repository.save(entity);
        List<InspectionItem> items = synchronizeItems(
            entity.getId().getCompanyId(),
            inspectionId,
            request.items()
        );
        return InspectionResponse.from(saved, items);
    }

    public void delete(String inspectionId) {
        Inspection entity = getExisting(inspectionId);
        itemRepository.deleteByIdCompanyIdAndIdInspectionId(
            entity.getId().getCompanyId(),
            inspectionId
        );
        repository.delete(entity);
    }

    private Inspection getExisting(String inspectionId) {
        return repository
            .findByIdCompanyIdAndIdInspectionId(MemberUserDetailsService.DEFAULT_COMPANY, inspectionId)
            .orElseThrow(() -> new NotFoundException("Inspection not found: " + inspectionId));
    }

    private void applyRequest(Inspection entity, InspectionRequest request) {
        entity.setName(request.name());
        entity.setPlantId(request.plantId());
        entity.setJobId(request.jobId());
        entity.setSiteId(request.siteId());
        entity.setDeptId(request.deptId());
        entity.setMemberId(request.memberId());
        entity.setPlannedDate(request.plannedDate());
        entity.setActualDate(request.actualDate());
        
        // ⭐ status/stage는 사용자 입력으로 변경 불가 (워크플로우로만 변경)
        // entity.setStatus(request.status());
        // entity.setStage(request.stage());
        
        entity.setRefEntity(request.refEntity());
        entity.setRefId(request.refId());
        entity.setRefStage(request.refStage());
        entity.setApprovalId(request.approvalId());
        entity.setFileGroupId(request.fileGroupId());
        entity.setNote(request.note());
    }

    private List<InspectionItem> synchronizeItems(
        String companyId,
        String inspectionId,
        List<InspectionItemRequest> items
    ) {
        itemRepository.deleteByIdCompanyIdAndIdInspectionId(companyId, inspectionId);
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<InspectionItem> entities = IntStream
            .range(0, items.size())
            .mapToObj(index -> toItemEntity(companyId, inspectionId, index + 1, items.get(index)))
            .collect(Collectors.toList());
        return itemRepository.saveAll(entities);
    }

    private InspectionItem toItemEntity(
        String companyId,
        String inspectionId,
        int lineNo,
        InspectionItemRequest item
    ) {
        InspectionItem entity = new InspectionItem();
        entity.setId(new InspectionItemId(companyId, inspectionId, lineNo));
        entity.setName(item.name());
        entity.setMethod(item.method());
        entity.setMinVal(item.minVal());
        entity.setMaxVal(item.maxVal());
        entity.setStdVal(item.stdVal());
        entity.setUnit(item.unit());
        entity.setResultVal(item.resultVal());
        entity.setNote(item.note());
        return entity;
    }

    private String resolveId(String companyId, String requestedId, LocalDate referenceDate) {
        if (requestedId != null && !requestedId.isBlank()) {
            String trimmed = requestedId.trim();
            repository
                .findByIdCompanyIdAndIdInspectionId(companyId, trimmed)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Inspection already exists: " + trimmed);
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
     * 계획 결재 상신
     */
    public ApprovalResponse submitPlanApproval(String inspectionId) {
        Inspection inspection = getExisting(inspectionId);
        
        // 상태 검증
        if (!"PLN".equals(inspection.getStage()) || !"DRAFT".equals(inspection.getStatus())) {
            throw new IllegalStateException("작성 중인 계획만 결재 요청 가능합니다. 현재 단계/상태: " + inspection.getStage() + "/" + inspection.getStatus());
        }
        
        // 결재 본문 자동 생성
        String content = buildPlanApprovalContent(inspection);
        
        // 빈 결재선으로 Approval 생성
        ApprovalRequest request = new ApprovalRequest(
            null,           // approvalId
            "점검 계획 결재: " + inspection.getName(),  // title
            "DRAFT",        // status
            "INSP",         // refEntity
            inspectionId,   // refId
            "PLN",          // refStage
            content,        // content
            inspection.getFileGroupId(),  // fileGroupId
            new ArrayList<>()  // steps
        );
        
        ApprovalResponse approval = approvalService.create(request);
        
        // Inspection 업데이트
        inspection.setApprovalId(approval.approvalId());
        inspection.setStatus("SUBMT");
        inspection.setUpdatedAt(LocalDateTime.now());
        inspection.setUpdatedBy(currentMemberId());
        repository.save(inspection);
        
        return approval;
    }

    /**
     * 계획 결재 승인 콜백
     */
    public void onPlanApprovalApprove(String inspectionId) {
        Inspection inspection = getExisting(inspectionId);
        inspection.setStatus("APPRV");
        inspection.setUpdatedAt(LocalDateTime.now());
        inspection.setUpdatedBy(currentMemberId());
        repository.save(inspection);
    }

    /**
     * 계획 결재 반려 콜백
     */
    public void onPlanApprovalReject(String inspectionId) {
        Inspection inspection = getExisting(inspectionId);
        inspection.setStatus("REJCT");
        inspection.setUpdatedAt(LocalDateTime.now());
        inspection.setUpdatedBy(currentMemberId());
        repository.save(inspection);
    }

    /**
     * 계획 결재 삭제 콜백
     */
    public void onPlanApprovalDelete(String inspectionId) {
        Inspection inspection = getExisting(inspectionId);
        inspection.setStatus("DRAFT");
        inspection.setApprovalId(null);
        inspection.setUpdatedAt(LocalDateTime.now());
        inspection.setUpdatedBy(currentMemberId());
        repository.save(inspection);
    }

    /**
     * 계획 자체 확정 (결재 없이 DRAFT → CMPLT)
     */
    public void onPlanApprovalComplete(String inspectionId) {
        Inspection inspection = getExisting(inspectionId);
        if (!"PLN".equals(inspection.getStage()) || !"DRAFT".equals(inspection.getStatus())) {
            throw new IllegalStateException("작성 중인 계획만 확정 가능합니다.");
        }
        inspection.setStatus("CMPLT");
        inspection.setUpdatedAt(LocalDateTime.now());
        inspection.setUpdatedBy(currentMemberId());
        repository.save(inspection);
    }

    /**
     * 계획 결재 문서 생성
     */
    private String buildPlanApprovalContent(Inspection inspection) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>점검 계획 결재 요청</h3>");
        sb.append("<table style='border-collapse:collapse; width:100%;'>");
        sb.append("<tr><th style='border:1px solid #ddd; padding:8px; background:#f5f5f5;'>항목</th>");
        sb.append("<th style='border:1px solid #ddd; padding:8px; background:#f5f5f5;'>내용</th></tr>");
        
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>점검 ID</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getId().getInspectionId()).append("</td></tr>");
        
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>점검명</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getName()).append("</td></tr>");
        
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>설비</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getPlantId() != null ? inspection.getPlantId() : "-").append("</td></tr>");
        
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>담당자</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getMemberId() != null ? inspection.getMemberId() : "-").append("</td></tr>");
        
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>계획일</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getPlannedDate() != null ? inspection.getPlannedDate().toString() : "-").append("</td></tr>");
        
        sb.append("</table>");
        
        if (inspection.getNote() != null && !inspection.getNote().isEmpty()) {
            sb.append("<p style='margin-top:15px;'><strong>비고:</strong></p>");
            sb.append("<p>").append(inspection.getNote()).append("</p>");
        }
        
        return sb.toString();
    }

    /**
     * 실적 결재 상신 (빈 결재선)
     */
    public ApprovalResponse submitActualApproval(String inspectionId) {
        Inspection inspection = getExisting(inspectionId);
        
        // 상태 검증
        if (!"ACT".equals(inspection.getStage()) || !"DRAFT".equals(inspection.getStatus())) {
            throw new IllegalStateException("작성 중인 점검만 결재 요청 가능합니다. 현재 단계/상태: " + inspection.getStage() + "/" + inspection.getStatus());
        }
        
        // 결재 본문 자동 생성
        String content = buildActualApprovalContent(inspection);
        
        // 빈 결재선으로 Approval 생성
        ApprovalRequest request = new ApprovalRequest(
            null,           // approvalId
            "점검 결재: " + inspection.getName(),  // title
            "DRAFT",        // status
            "INSP",         // refEntity
            inspectionId,   // refId
            "ACT",          // refStage
            content,        // content
            inspection.getFileGroupId(),  // fileGroupId
            new ArrayList<>()  // steps
        );
        
        ApprovalResponse approval = approvalService.create(request);
        
        // Inspection 업데이트
        inspection.setApprovalId(approval.approvalId());
        inspection.setStatus("SUBMT");
        inspection.setUpdatedAt(LocalDateTime.now());
        inspection.setUpdatedBy(currentMemberId());
        repository.save(inspection);
        
        return approval;
    }

    /**
     * 실적 결재 승인 콜백
     */
    public void onActualApprovalApprove(String inspectionId) {
        Inspection inspection = getExisting(inspectionId);
        inspection.setStatus("APPRV");
        inspection.setUpdatedAt(LocalDateTime.now());
        inspection.setUpdatedBy(currentMemberId());
        repository.save(inspection);
    }

    /**
     * 실적 결재 반려 콜백
     */
    public void onActualApprovalReject(String inspectionId) {
        Inspection inspection = getExisting(inspectionId);
        inspection.setStatus("REJCT");
        inspection.setUpdatedAt(LocalDateTime.now());
        inspection.setUpdatedBy(currentMemberId());
        repository.save(inspection);
    }

    /**
     * 실적 결재 삭제 콜백
     */
    public void onActualApprovalDelete(String inspectionId) {
        Inspection inspection = getExisting(inspectionId);
        inspection.setStatus("DRAFT");
        inspection.setApprovalId(null);
        inspection.setUpdatedAt(LocalDateTime.now());
        inspection.setUpdatedBy(currentMemberId());
        repository.save(inspection);
    }

    /**
     * 실적 자체 확정 (결재 없이 DRAFT → CMPLT)
     */
    public void onActualApprovalComplete(String inspectionId) {
        Inspection inspection = getExisting(inspectionId);
        if (!"ACT".equals(inspection.getStage()) || !"DRAFT".equals(inspection.getStatus())) {
            throw new IllegalStateException("작성 중인 실적만 확정 가능합니다.");
        }
        inspection.setStatus("CMPLT");
        inspection.setUpdatedAt(LocalDateTime.now());
        inspection.setUpdatedBy(currentMemberId());
        repository.save(inspection);
    }

    /**
     * @deprecated Use {@link #onActualApprovalApprove(String)} instead
     */
    @Deprecated
    public void onApprovalComplete(String inspectionId) {
        onActualApprovalApprove(inspectionId);
    }

    /**
     * @deprecated Use {@link #onActualApprovalReject(String)} instead
     */
    @Deprecated
    public void onApprovalReject(String inspectionId) {
        onActualApprovalReject(inspectionId);
    }

    /**
     * @deprecated Use {@link #onActualApprovalDelete(String)} instead
     */
    @Deprecated
    public void onApprovalDelete(String inspectionId) {
        onActualApprovalDelete(inspectionId);
    }

    /**
     * 실적 입력 단계 준비 (PLN+APPRV → ACT+DRAFT)
     */
    public void prepareActualStage(String inspectionId) {
        Inspection inspection = getExisting(inspectionId);
        
        // 상태 검증
        if (!"PLN".equals(inspection.getStage()) || !"APPRV".equals(inspection.getStatus())) {
            throw new IllegalStateException(
                "계획이 확정되어야 실적을 입력할 수 있습니다. 현재 단계/상태: " + 
                inspection.getStage() + "/" + inspection.getStatus()
            );
        }
        
        // 상태 전환
        inspection.setStage("ACT");
        inspection.setStatus("DRAFT");
        inspection.setUpdatedAt(LocalDateTime.now());
        inspection.setUpdatedBy(currentMemberId());
        repository.save(inspection);
    }

    /**
     * 점검 항목 조회
     */
    @Transactional(readOnly = true)
    public List<InspectionItem> getItems(String inspectionId) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        return itemRepository.findByIdCompanyIdAndIdInspectionIdOrderByIdLineNo(companyId, inspectionId);
    }

    /**
     * 실적 결재 본문 생성
     */
    private String buildActualApprovalContent(Inspection inspection) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>점검 실적 결재 요청</h3>");
        sb.append("<table style='border-collapse:collapse; width:100%;'>");
        sb.append("<tr><th style='border:1px solid #ddd; padding:8px; background:#f5f5f5;'>항목</th>");
        sb.append("<th style='border:1px solid #ddd; padding:8px; background:#f5f5f5;'>내용</th></tr>");
        
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>점검 ID</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getId().getInspectionId()).append("</td></tr>");
        
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>점검명</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getName()).append("</td></tr>");
        
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>설비</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getPlantId() != null ? inspection.getPlantId() : "-").append("</td></tr>");
        
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>담당자</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getMemberId() != null ? inspection.getMemberId() : "-").append("</td></tr>");
        
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>계획일</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getPlannedDate() != null ? inspection.getPlannedDate().toString() : "-").append("</td></tr>");
        
        sb.append("<tr><td style='border:1px solid #ddd; padding:8px;'>실적일</td>");
        sb.append("<td style='border:1px solid #ddd; padding:8px;'>").append(inspection.getActualDate() != null ? inspection.getActualDate().toString() : "-").append("</td></tr>");
        
        sb.append("</table>");
        
        if (inspection.getNote() != null && !inspection.getNote().isEmpty()) {
            sb.append("<p style='margin-top:15px;'><strong>비고:</strong></p>");
            sb.append("<p>").append(inspection.getNote()).append("</p>");
        }
        
        return sb.toString();
    }
}
