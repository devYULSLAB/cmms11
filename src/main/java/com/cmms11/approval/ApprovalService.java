package com.cmms11.approval;

import com.cmms11.common.error.NotFoundException;
import com.cmms11.common.seq.AutoNumberService;
import com.cmms11.inspection.InspectionService;
import com.cmms11.security.MemberUserDetailsService;
import com.cmms11.workorder.WorkOrderService;
import com.cmms11.workpermit.WorkPermitService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이름: ApprovalService
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 결재 헤더 및 단계 CRUD 로직을 담당하는 서비스.
 */
@Service
@Transactional
public class ApprovalService {

    private static final String MODULE_CODE = "A";
    private static final String INBOX_MODULE_CODE = "I";

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_SUBMITTED = "SUBMT";
    private static final String STATUS_IN_PROGRESS = "PROC";
    private static final String STATUS_APPROVED = "APPRV";
    private static final String STATUS_REJECTED = "REJCT";

    private static final String INBOX_SUBMITTED = "SUBMT";
    private static final String INBOX_APPROVED = "APPRV";
    private static final String INBOX_REJECTED = "REJCT";
    private static final String INBOX_COMPLETED = "CMPLT";

    private static final String DECISION_APPROVAL = "APPRL";
    private static final String DECISION_AGREE = "AGREE";
    private static final String DECISION_INFO = "INFO";

    private static final String RESULT_APPROVE = "APPROVE";
    private static final String RESULT_REJECT = "REJECT";

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    private final ApprovalRepository repository;
    private final ApprovalStepRepository stepRepository;
    private final ApprovalInboxRepository inboxRepository;
    private final AutoNumberService autoNumberService;
    
    @Autowired
    private InspectionService inspectionService;
    
    @Autowired
    private WorkOrderService workOrderService;
    
    @Autowired
    private WorkPermitService workPermitService;

    public ApprovalService(
        ApprovalRepository repository,
        ApprovalStepRepository stepRepository,
        ApprovalInboxRepository inboxRepository,
        AutoNumberService autoNumberService
    ) {
        this.repository = repository;
        this.stepRepository = stepRepository;
        this.inboxRepository = inboxRepository;
        this.autoNumberService = autoNumberService;
    }

    @Transactional(readOnly = true)
    public Page<ApprovalResponse> list(String keyword, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Page<Approval> page;
        if (keyword == null || keyword.isBlank()) {
            page = repository.findByIdCompanyId(companyId, pageable);
        } else {
            String trimmed = "%" + keyword.trim() + "%";
            page = repository.search(companyId, trimmed, pageable);
        }
        return page.map(this::toResponseWithoutSteps);
    }

    @Transactional(readOnly = true)
    public Page<ApprovalResponse> listByStatus(String status, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Page<Approval> page;
        if (status == null || status.isBlank()) {
            page = repository.findByIdCompanyId(companyId, pageable);
        } else {
            page = repository.findByFilters(companyId, null, null, status, pageable);
        }
        return page.map(this::toResponseWithoutSteps);
    }

    @Transactional(readOnly = true)
    public Page<ApprovalResponse> list(String title, String createdBy, String status, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;

        Page<Approval> page = repository.findByFilters(
            companyId, 
            title, 
            createdBy, 
            status, 
            pageable
        );
        
        return page.map(this::toResponseWithoutSteps);
    }

    /**
     * 미결함: 내가 결재해야 할 문서
     */
    @Transactional(readOnly = true)
    public Page<ApprovalResponse> findPendingApprovals(String memberId, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Page<Approval> page = repository.findPendingByMemberId(companyId, memberId, pageable);
        return page.map(this::toResponseWithoutSteps);
    }

    /**
     * 기결함: 내가 승인/합의한 문서
     */
    @Transactional(readOnly = true)
    public Page<ApprovalResponse> findApprovedApprovals(String memberId, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Page<Approval> page = repository.findApprovedByMemberId(companyId, memberId, pageable);
        return page.map(this::toResponseWithoutSteps);
    }

    /**
     * 반려함: 내가 반려한 문서
     */
    @Transactional(readOnly = true)
    public Page<ApprovalResponse> findRejectedApprovals(String memberId, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Page<Approval> page = repository.findRejectedByMemberId(companyId, memberId, pageable);
        return page.map(this::toResponseWithoutSteps);
    }

    /**
     * 상신함: 내가 상신한 문서
     */
    @Transactional(readOnly = true)
    public Page<ApprovalResponse> findSentApprovals(String memberId, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Page<Approval> page = repository.findSentByMemberId(companyId, memberId, pageable);
        return page.map(this::toResponseWithoutSteps);
    }

    /**
     * 목록 조회 시 단계 정보 제외한 응답 생성 (성능 최적화)
     */
    private ApprovalResponse toResponseWithoutSteps(Approval approval) {
        return ApprovalResponse.from(approval, Collections.emptyList());
    }

    @Transactional(readOnly = true)
    public ApprovalResponse get(String approvalId) {
        Approval approval = getExisting(approvalId);
        List<ApprovalStepResponse> steps = stepRepository
            .findByIdCompanyIdAndIdApprovalIdOrderByIdStepNo(MemberUserDetailsService.DEFAULT_COMPANY, approvalId)
            .stream()
            .map(ApprovalStepResponse::from)
            .collect(Collectors.toList());
        return ApprovalResponse.from(approval, steps);
    }

    public ApprovalResponse create(ApprovalRequest request) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        LocalDateTime now = LocalDateTime.now();
        String memberId = currentMemberId();

        String newId = resolveId(companyId, request.approvalId());
        Approval entity = new Approval();
        entity.setId(new ApprovalId(companyId, newId));
        entity.setCreatedAt(now);
        entity.setCreatedBy(memberId);
        applyRequest(entity, request);
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(memberId);
        Approval saved = repository.save(entity);

        replaceSteps(saved, request.steps());
        List<ApprovalStepResponse> steps = stepRepository
            .findByIdCompanyIdAndIdApprovalIdOrderByIdStepNo(companyId, newId)
            .stream()
            .map(ApprovalStepResponse::from)
            .collect(Collectors.toList());
        return ApprovalResponse.from(saved, steps);
    }

    public ApprovalResponse update(String approvalId, ApprovalRequest request) {
        Approval entity = getExisting(approvalId);
        applyRequest(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentMemberId());
        Approval saved = repository.save(entity);

        replaceSteps(saved, request.steps());
        List<ApprovalStepResponse> steps = stepRepository
            .findByIdCompanyIdAndIdApprovalIdOrderByIdStepNo(MemberUserDetailsService.DEFAULT_COMPANY, approvalId)
            .stream()
            .map(ApprovalStepResponse::from)
            .collect(Collectors.toList());
        return ApprovalResponse.from(saved, steps);
    }

    public void delete(String approvalId) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Approval entity = getExisting(approvalId);
        
        // 원본 모듈 상태 복원 콜백 (DRAFT로 되돌림)
        notifyRefModule(entity, "DELETE");
        
        stepRepository.deleteByIdCompanyIdAndIdApprovalId(companyId, approvalId);
        inboxRepository.deleteByIdCompanyIdAndApprovalId(companyId, approvalId);
        repository.delete(entity);
    }

    public ApprovalResponse approve(String approvalId, String comment) {
        return processApproval(approvalId, comment, RESULT_APPROVE);
    }

    public ApprovalResponse reject(String approvalId, String comment) {
        return processApproval(approvalId, comment, RESULT_REJECT);
    }

    /**
     * 승인/반려 공통 처리 (중복 제거)
     */
    private ApprovalResponse processApproval(
        String approvalId,
        String comment,
        String stepResult
    ) {
        Approval entity = getExisting(approvalId);
        LocalDateTime now = LocalDateTime.now();
        String memberId = currentMemberId();

        // 결재 상태 확인
        if (!STATUS_SUBMITTED.equals(entity.getStatus()) && !STATUS_IN_PROGRESS.equals(entity.getStatus())) {
            throw new IllegalStateException("결재 대기 중인 문서만 처리할 수 있습니다. 현재 상태: " + entity.getStatus());
        }

        // ⭐ 스텝 1회만 조회
        List<ApprovalStep> steps = stepRepository
            .findByIdCompanyIdAndIdApprovalIdOrderByIdStepNo(entity.getCompanyId(), approvalId);

        ApprovalStep targetStep = steps.stream()
            .filter(step -> memberId.equals(step.getMemberId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("결재 권한이 없습니다"));

        if (targetStep.getDecidedAt() != null) {
            throw new IllegalStateException("이미 결재한 문서입니다");
        }

        enforceStepOrder(steps, targetStep);

        targetStep.setDecidedAt(now);
        targetStep.setComment(comment);
        targetStep.setResult(stepResult);
        stepRepository.save(targetStep);

        String nextStatus;
        boolean shouldNotify = false;

        if (RESULT_REJECT.equals(stepResult) && !DECISION_INFO.equals(targetStep.getDecision())) {
            nextStatus = STATUS_REJECTED;
            entity.setCompletedAt(now);
            shouldNotify = true;
        } else if (checkAllApproversCompleted(steps)) {
            nextStatus = STATUS_APPROVED;
            entity.setCompletedAt(now);
            shouldNotify = true;
        } else {
            nextStatus = STATUS_IN_PROGRESS;
            entity.setCompletedAt(null);
        }

        entity.setStatus(nextStatus);
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(memberId);

        Approval saved = repository.save(entity);

        updateInboxAfterDecision(saved, targetStep, stepResult, now);

        if (shouldNotify) {
            notifyRefModule(saved, nextStatus);
        }

        // 응답 생성 (이미 로드된 steps 재사용)
        List<ApprovalStepResponse> stepResponses = steps.stream()
            .map(ApprovalStepResponse::from)
            .collect(Collectors.toList());
        
        log.info("결재 처리 완료: approvalId={}, result={}", approvalId, stepResult);
        
        return ApprovalResponse.from(saved, stepResponses);
    }

    /**
     * 결재선 입력 후 상신 처리 (DRAFT → SUBMIT)
     */
    public ApprovalResponse submit(String approvalId, List<ApprovalStepRequest> steps) {
        Approval entity = getExisting(approvalId);
        LocalDateTime now = LocalDateTime.now();
        String memberId = currentMemberId();

        // 상태 검증
        if (!STATUS_DRAFT.equals(entity.getStatus())) {
            throw new IllegalStateException("임시저장 상태에서만 상신할 수 있습니다. 현재 상태: " + entity.getStatus());
        }

        // 상태 전환: DRAFT → SUBMT
        entity.setStatus(STATUS_SUBMITTED);
        entity.setSubmittedAt(now);
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(memberId);

        // 결재선 등록 (제공된 경우에만)
        if (steps != null && !steps.isEmpty()) {
            replaceSteps(entity, steps);
        } else {
            syncInboxMetadata(entity, now, memberId);
        }

        Approval saved = repository.save(entity);

        // 응답 생성
        List<ApprovalStepResponse> stepResponses = stepRepository
            .findByIdCompanyIdAndIdApprovalIdOrderByIdStepNo(entity.getCompanyId(), approvalId)
            .stream()
            .map(ApprovalStepResponse::from)
            .collect(Collectors.toList());
            
        log.info("결재 상신 완료: approvalId={}, steps={}", approvalId, stepResponses.size());
        
        return ApprovalResponse.from(saved, stepResponses);
    }

    private Approval getExisting(String approvalId) {
        return repository
            .findByIdCompanyIdAndIdApprovalId(MemberUserDetailsService.DEFAULT_COMPANY, approvalId)
            .orElseThrow(() -> new NotFoundException("Approval not found: " + approvalId));
    }

    private void applyRequest(Approval entity, ApprovalRequest request) {
        entity.setTitle(request.title());
        entity.setStatus(request.status());
        entity.setRefEntity(request.refEntity());
        entity.setRefId(request.refId());
        entity.setRefStage(request.refStage());  // ⭐ 추가: 결재 단계 저장
        
        entity.setContent(request.content());
        entity.setFileGroupId(request.fileGroupId());
    }

    private void replaceSteps(Approval approval, List<ApprovalStepRequest> steps) {
        String companyId = approval.getCompanyId();
        String approvalId = approval.getApprovalId();

        stepRepository.deleteByIdCompanyIdAndIdApprovalId(companyId, approvalId);
        inboxRepository.deleteByIdCompanyIdAndApprovalId(companyId, approvalId);

        if (steps == null || steps.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < steps.size(); i++) {
            ApprovalStepRequest request = steps.get(i);
            int stepNo = request.stepNo() != null && request.stepNo() > 0 ? request.stepNo() : (i + 1);
            ApprovalStep step = new ApprovalStep();
            step.setId(new ApprovalStepId(companyId, approvalId, stepNo));
            step.setMemberId(request.memberId());
            step.setDecision(request.decision());
            step.setResult(request.result());
            step.setDecidedAt(request.decidedAt());
            step.setComment(request.comment());
            stepRepository.save(step);

            createInboxEntry(approval, step, now);
        }
    }

    private void createInboxEntry(Approval approval, ApprovalStep step, LocalDateTime createdAt) {
        if (step.getMemberId() == null) {
            return;
        }

        String companyId = approval.getCompanyId();
        String approvalId = approval.getApprovalId();
        String inboxId = autoNumberService.generateTxId(companyId, INBOX_MODULE_CODE, LocalDate.now());

        ApprovalInbox inbox = new ApprovalInbox();
        inbox.setId(new ApprovalInboxId(companyId, inboxId));
        inbox.setMemberId(step.getMemberId());
        inbox.setApprovalId(approvalId);
        inbox.setStepNo(step.getId() != null ? step.getId().getStepNo() : null);
        inbox.setInboxType(INBOX_SUBMITTED);
        inbox.setIsRead("N");
        inbox.setDecision(step.getDecision());
        inbox.setTitle(approval.getTitle());
        inbox.setRefEntity(approval.getRefEntity());
        inbox.setRefId(approval.getRefId());
        inbox.setSubmittedBy(approval.getUpdatedBy() != null ? approval.getUpdatedBy() : approval.getCreatedBy());
        inbox.setSubmittedAt(approval.getSubmittedAt());
        inbox.setCreatedAt(createdAt);
        inboxRepository.save(inbox);
    }

    private void syncInboxMetadata(Approval approval, LocalDateTime submittedAt, String submittedBy) {
        if (approval.getCompanyId() == null || approval.getApprovalId() == null) {
            return;
        }

        List<ApprovalInbox> inboxes = inboxRepository
            .findByIdCompanyIdAndApprovalId(approval.getCompanyId(), approval.getApprovalId());

        LocalDateTime now = LocalDateTime.now();
        for (ApprovalInbox inbox : inboxes) {
            inbox.setTitle(approval.getTitle());
            inbox.setRefEntity(approval.getRefEntity());
            inbox.setRefId(approval.getRefId());
            inbox.setSubmittedAt(submittedAt);
            inbox.setSubmittedBy(submittedBy);
            if (inbox.getInboxType() == null) {
                inbox.setInboxType(INBOX_SUBMITTED);
            }
            inbox.setUpdatedAt(now);
            inboxRepository.save(inbox);
        }
    }

    private void updateInboxAfterDecision(
        Approval approval,
        ApprovalStep step,
        String stepResult,
        LocalDateTime now
    ) {
        if (approval.getCompanyId() == null || approval.getApprovalId() == null || step.getMemberId() == null) {
            return;
        }

        Optional<ApprovalInbox> inboxOptional = inboxRepository
            .findByIdCompanyIdAndApprovalIdAndMemberId(
                approval.getCompanyId(),
                approval.getApprovalId(),
                step.getMemberId()
            );

        if (inboxOptional.isEmpty()) {
            return;
        }

        ApprovalInbox inbox = inboxOptional.get();

        if (DECISION_INFO.equals(step.getDecision())) {
            inbox.setInboxType(INBOX_COMPLETED);
        } else if (RESULT_REJECT.equals(stepResult)) {
            inbox.setInboxType(INBOX_REJECTED);
        } else {
            inbox.setInboxType(INBOX_APPROVED);
        }

        inbox.setIsRead("Y");
        inbox.setReadAt(now);
        inbox.setUpdatedAt(now);
        inboxRepository.save(inbox);
    }

    private void enforceStepOrder(List<ApprovalStep> steps, ApprovalStep targetStep) {
        if (DECISION_INFO.equals(targetStep.getDecision())) {
            return;
        }

        int targetNo = targetStep.getId() != null ? targetStep.getId().getStepNo() : 0;
        for (ApprovalStep step : steps) {
            if (step.getId() == null || step.getId().getStepNo() >= targetNo) {
                continue;
            }
            if (!isSequentialDecision(step.getDecision())) {
                continue;
            }
            if (step.getDecidedAt() == null) {
                throw new IllegalStateException(
                    String.format(
                        "이전 결재자(%d번: %s)가 먼저 결재해야 합니다",
                        step.getId().getStepNo(),
                        step.getMemberId()
                    )
                );
            }
            if (RESULT_REJECT.equals(step.getResult())) {
                throw new IllegalStateException(
                    String.format("%d번 결재자가 반려하여 결재가 종료되었습니다", step.getId().getStepNo())
                );
            }
        }
    }

    private boolean checkAllApproversCompleted(List<ApprovalStep> steps) {
        return steps.stream()
            .filter(step -> isSequentialDecision(step.getDecision()))
            .allMatch(step -> step.getDecidedAt() != null && RESULT_APPROVE.equals(step.getResult()));
    }

    private boolean isSequentialDecision(String decision) {
        return DECISION_APPROVAL.equals(decision) || DECISION_AGREE.equals(decision);
    }

    @Transactional
    public void markInboxAsRead(String inboxId) {
        markInboxAsRead(MemberUserDetailsService.DEFAULT_COMPANY, inboxId, currentMemberId());
    }

    @Transactional
    public void markInboxAsRead(String companyId, String inboxId, String memberId) {
        ApprovalInbox inbox = inboxRepository
            .findById(new ApprovalInboxId(companyId, inboxId))
            .orElseThrow(() -> new IllegalArgumentException("Inbox를 찾을 수 없습니다: " + inboxId));

        if (inbox.getMemberId() == null || !inbox.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 Inbox만 읽음 처리할 수 있습니다.");
        }

        if ("Y".equals(inbox.getIsRead())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        inbox.setIsRead("Y");
        inbox.setReadAt(now);
        inbox.setUpdatedAt(now);
        inboxRepository.save(inbox);
    }

    public long getUnreadInboxCount() {
        return getUnreadInboxCount(MemberUserDetailsService.DEFAULT_COMPANY, currentMemberId());
    }

    public long getUnreadInboxCount(String companyId, String memberId) {
        return inboxRepository.countByIdCompanyIdAndMemberIdAndIsRead(companyId, memberId, "N");
    }

    public long countInboxByType(String companyId, String memberId, String inboxType) {
        return inboxRepository.countByIdCompanyIdAndMemberIdAndInboxType(companyId, memberId, inboxType);
    }

    public long countInboxByType(String inboxType) {
        return countInboxByType(MemberUserDetailsService.DEFAULT_COMPANY, currentMemberId(), inboxType);
    }

    public Page<ApprovalInboxResponse> getMyInbox(String type, Pageable pageable) {
        return getMyInbox(MemberUserDetailsService.DEFAULT_COMPANY, currentMemberId(), type, pageable);
    }

    public Page<ApprovalInboxResponse> getMyInbox(
        String companyId,
        String memberId,
        String type,
        Pageable pageable
    ) {
        Page<ApprovalInbox> page;
        if (type == null || type.isBlank()) {
            page = inboxRepository.findByIdCompanyIdAndMemberId(companyId, memberId, pageable);
        } else {
            page = inboxRepository.findByIdCompanyIdAndMemberIdAndInboxType(companyId, memberId, type, pageable);
        }
        return page.map(ApprovalInboxResponse::from);
    }

    public Optional<ApprovalInboxResponse> getMyInboxByApproval(String approvalId) {
        return getMyInboxByApproval(MemberUserDetailsService.DEFAULT_COMPANY, approvalId, currentMemberId());
    }

    public Optional<ApprovalInboxResponse> getMyInboxByApproval(
        String companyId,
        String approvalId,
        String memberId
    ) {
        return inboxRepository
            .findByIdCompanyIdAndApprovalIdAndMemberId(companyId, approvalId, memberId)
            .map(ApprovalInboxResponse::from);
    }

    private String resolveId(String companyId, String requestedId) {
        if (requestedId != null && !requestedId.isBlank()) {
            String trimmed = requestedId.trim();
            repository
                .findByIdCompanyIdAndIdApprovalId(companyId, trimmed)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Approval already exists: " + trimmed);
                });
            return trimmed;
        }
        return autoNumberService.generateTxId(companyId, MODULE_CODE, LocalDate.now());
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
     * 원본 모듈에 상태 변경 통보
     */
    private void notifyRefModule(Approval approval, String action) {
        if (approval.getRefEntity() == null || approval.getRefId() == null) {
            return;
        }
        
        try {
            String refEntity = approval.getRefEntity();
            String refId = approval.getRefId();
            String refStage = approval.getRefStage();
            
            switch (refEntity) {
                case "INSP":
                    // ⭐ Stage 분기 필수 (PLN/ACT)
                    if (refStage == null) {
                        log.warn("INSP 모듈 콜백 시 refStage가 NULL입니다: {}", refId);
                        break;
                    }
                    
                    if ("PLN".equals(refStage)) {
                        if (STATUS_APPROVED.equals(action)) {
                            inspectionService.onPlanApprovalApprove(refId);
                        } else if (STATUS_REJECTED.equals(action)) {
                            inspectionService.onPlanApprovalReject(refId);
                        } else if ("DELETE".equals(action)) {
                            inspectionService.onPlanApprovalDelete(refId);
                        }
                        log.info("Inspection 계획 결재 콜백 완료: {} - {}", refId, action);

                    } else if ("ACT".equals(refStage)) {
                        if (STATUS_APPROVED.equals(action)) {
                            inspectionService.onActualApprovalApprove(refId);
                        } else if (STATUS_REJECTED.equals(action)) {
                            inspectionService.onActualApprovalReject(refId);
                        } else if ("DELETE".equals(action)) {
                            inspectionService.onActualApprovalDelete(refId);
                        }
                        log.info("Inspection 실적 결재 콜백 완료: {} - {}", refId, action);
                        
                    } else {
                        log.warn("알 수 없는 INSP refStage: {} (refId={})", refStage, refId);
                    }
                    break;
                    
                case "WORK":
                    // ⭐ Stage 분기 필수 (PLN/ACT)
                    if (refStage == null) {
                        log.warn("WORK 모듈 콜백 시 refStage가 NULL입니다: {}", refId);
                        break;
                    }
                    
                    if ("PLN".equals(refStage)) {
                        if (STATUS_APPROVED.equals(action)) {
                            workOrderService.onPlanApprovalApprove(refId);
                        } else if (STATUS_REJECTED.equals(action)) {
                            workOrderService.onPlanApprovalReject(refId);
                        } else if ("DELETE".equals(action)) {
                            workOrderService.onPlanApprovalDelete(refId);
                        }
                        log.info("WorkOrder 계획 결재 콜백 완료: {} - {}", refId, action);

                    } else if ("ACT".equals(refStage)) {
                        if (STATUS_APPROVED.equals(action)) {
                            workOrderService.onActualApprovalApprove(refId);
                        } else if (STATUS_REJECTED.equals(action)) {
                            workOrderService.onActualApprovalReject(refId);
                        } else if ("DELETE".equals(action)) {
                            workOrderService.onActualApprovalDelete(refId);
                        }
                        log.info("WorkOrder 실적 결재 콜백 완료: {} - {}", refId, action);
                        
                    } else {
                        log.warn("알 수 없는 WORK refStage: {} (refId={})", refStage, refId);
                    }
                    break;
                    
                case "WPER":
                    // WorkPermit은 계획(PLN)만 있음
                    if (STATUS_APPROVED.equals(action)) {
                        workPermitService.onPlanApprovalApprove(refId);
                    } else if (STATUS_REJECTED.equals(action)) {
                        workPermitService.onPlanApprovalReject(refId);
                    } else if ("DELETE".equals(action)) {
                        workPermitService.onPlanApprovalDelete(refId);
                    }
                    log.info("WorkPermit 계획 결재 콜백 완료: {} - {}", refId, action);
                    break;
                    
                default:
                    log.warn("처리되지 않은 ref_entity: {}", refEntity);
            }
        } catch (Exception e) {
            log.error("원본 모듈 콜백 실패: refEntity={}, refId={}, refStage={}, action={}", 
                approval.getRefEntity(), approval.getRefId(), approval.getRefStage(), action, e);
        }
    }
}
