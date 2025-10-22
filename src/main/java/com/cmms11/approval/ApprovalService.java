package com.cmms11.approval;

import com.cmms11.common.error.NotFoundException;
import com.cmms11.common.seq.AutoNumberService;
import com.cmms11.security.MemberUserDetailsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결재 도메인 서비스. REST + Outbox 기반 구조에 맞춰 상신/처리/Inbox 연동을 담당한다.
 */
@Service
@Transactional
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    private static final String MODULE_CODE = "A";
    private static final String INBOX_MODULE_CODE = "I";

    private static final String STATUS_SUBMITTED = "SUBMT";
    private static final String STATUS_IN_PROGRESS = "PROC";
    private static final String STATUS_APPROVED = "APPRV";
    private static final String STATUS_REJECTED = "REJCT";
    private static final String STATUS_CANCELLED = "CNCLD";

    private static final String DECISION_APPROVAL = "APPRL";
    private static final String DECISION_AGREE = "AGREE";
    private static final String DECISION_INFO = "INFO";

    private static final String INBOX_SUBMITTED = "SUBMT";
    private static final String INBOX_APPROVED = "APPRV";
    private static final String INBOX_REJECTED = "REJCT";
    private static final String INBOX_COMPLETED = "CMPLT";

    private static final Pattern IDEMPOTENCY_KEY_PATTERN =
        Pattern.compile("^[A-Z0-9]{2,}_[A-Z0-9]+_[A-Z0-9]+_[A-Z0-9]+_[A-F0-9]{8}$");

    private final ApprovalRepository repository;
    private final ApprovalStepRepository stepRepository;
    private final ApprovalInboxRepository inboxRepository;
    private final ApprovalOutboxRepository outboxRepository;
    private final AutoNumberService autoNumberService;
    private final ObjectMapper objectMapper;

    public ApprovalService(
        ApprovalRepository repository,
        ApprovalStepRepository stepRepository,
        ApprovalInboxRepository inboxRepository,
        ApprovalOutboxRepository outboxRepository,
        AutoNumberService autoNumberService,
        ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.stepRepository = stepRepository;
        this.inboxRepository = inboxRepository;
        this.outboxRepository = outboxRepository;
        this.autoNumberService = autoNumberService;
        this.objectMapper = objectMapper;
    }

    // ===== 목록/조회 =====

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
        Page<Approval> page = repository.findByFilters(companyId, title, createdBy, status, pageable);
        return page.map(this::toResponseWithoutSteps);
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

    @Transactional(readOnly = true)
    public List<ApprovalStepResponse> getApprovalLine(String approvalId) {
        return stepRepository
            .findByIdCompanyIdAndIdApprovalIdOrderByIdStepNo(
                MemberUserDetailsService.DEFAULT_COMPANY,
                approvalId
            )
            .stream()
            .map(ApprovalStepResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApprovalStepResponse> getApprovalOpinions(String approvalId) {
        return getApprovalLine(approvalId).stream()
            .filter(step -> step.comment() != null && !step.comment().isBlank())
            .collect(Collectors.toList());
    }

    // ===== Inbox =====

    @Transactional(readOnly = true)
    public Page<ApprovalResponse> findPendingApprovals(String memberId, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Page<Approval> page = repository.findPendingByMemberId(companyId, memberId, pageable);
        return page.map(this::toResponseWithoutSteps);
    }

    @Transactional(readOnly = true)
    public Page<ApprovalResponse> findApprovedApprovals(String memberId, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Page<Approval> page = repository.findApprovedByMemberId(companyId, memberId, pageable);
        return page.map(this::toResponseWithoutSteps);
    }

    @Transactional(readOnly = true)
    public Page<ApprovalResponse> findRejectedApprovals(String memberId, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Page<Approval> page = repository.findRejectedByMemberId(companyId, memberId, pageable);
        return page.map(this::toResponseWithoutSteps);
    }

    @Transactional(readOnly = true)
    public Page<ApprovalResponse> findSentApprovals(String memberId, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Page<Approval> page = repository.findSentByMemberId(companyId, memberId, pageable);
        return page.map(this::toResponseWithoutSteps);
    }

    // ===== 상신/처리 =====

    public ApprovalResponse create(ApprovalRequest request) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        String currentMemberId = MemberUserDetailsService.getCurrentMemberId();
        String normalizedKey = normalizeIdempotencyKey(request.idempotencyKey());

        validateIdempotencyKey(companyId, normalizedKey);

        Optional<Approval> existing = repository.findByIdCompanyIdAndIdempotencyKey(companyId, normalizedKey);
        if (existing.isPresent()) {
            Approval approval = existing.get();
            List<ApprovalStepResponse> stepResponses = stepRepository
                .findByIdCompanyIdAndIdApprovalIdOrderByIdStepNo(companyId, approval.getApprovalId())
                .stream()
                .map(ApprovalStepResponse::from)
                .collect(Collectors.toList());
            return ApprovalResponse.from(approval, stepResponses);
        }

        LocalDateTime now = LocalDateTime.now();
        String approvalId = autoNumberService.generateTxId(companyId, MODULE_CODE, LocalDate.now());

        Approval approval = new Approval();
        approval.setId(new ApprovalId(companyId, approvalId));
        approval.setTitle(request.title());
        approval.setStatus(STATUS_SUBMITTED);
        approval.setRefEntity(request.refEntity());
        approval.setRefId(request.refId());
        approval.setRefStage(request.refStage());
        approval.setCallbackUrl(request.callbackUrl());
        approval.setIdempotencyKey(normalizedKey);
        approval.setContent(request.content());
        approval.setFileGroupId(request.fileGroupId());
        approval.setSubmittedAt(now);
        approval.setCreatedAt(now);
        approval.setCreatedBy(currentMemberId);
        approval.setUpdatedAt(now);
        approval.setUpdatedBy(currentMemberId);

        Approval saved = repository.save(approval);
        List<ApprovalStep> steps = persistSteps(saved, request.steps(), now, currentMemberId);

        enqueueOutbox(saved, steps, ApprovalEventType.SUBMITTED, now, currentMemberId, null);

        List<ApprovalStepResponse> stepResponses = steps.stream()
            .map(ApprovalStepResponse::from)
            .collect(Collectors.toList());
        return ApprovalResponse.from(saved, stepResponses);
    }

    public ApprovalResponse approve(String approvalId, String comment) {
        return processApproval(approvalId, comment, STATUS_APPROVED, ApprovalEventType.APPROVED);
    }

    public ApprovalResponse reject(String approvalId, String comment) {
        return processApproval(approvalId, comment, STATUS_REJECTED, ApprovalEventType.REJECTED);
    }

    public ApprovalResponse cancel(String approvalId, String comment) {
        Approval approval = getExisting(approvalId);
        if (!STATUS_SUBMITTED.equals(approval.getStatus()) && !STATUS_IN_PROGRESS.equals(approval.getStatus())) {
            throw new IllegalStateException("SUBMT/PROC 상태만 취소할 수 있습니다. 현재 상태: " + approval.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        String currentMemberId = MemberUserDetailsService.getCurrentMemberId();
        approval.setStatus(STATUS_CANCELLED);
        approval.setCompletedAt(now);
        approval.setUpdatedAt(now);
        approval.setUpdatedBy(currentMemberId);

        Approval saved = repository.save(approval);
        updateInboxAfterCancel(saved, now);

        List<ApprovalStep> steps = stepRepository
            .findByIdCompanyIdAndIdApprovalIdOrderByIdStepNo(saved.getCompanyId(), approvalId);
        enqueueOutbox(saved, steps, ApprovalEventType.CANCELLED, now, currentMemberId, comment);

        List<ApprovalStepResponse> stepResponses = steps.stream()
            .map(ApprovalStepResponse::from)
            .collect(Collectors.toList());
        return ApprovalResponse.from(saved, stepResponses);
    }

    // ===== Inbox API =====

    @Transactional
    public void markInboxAsRead(String inboxId) {
        markInboxAsRead(
            MemberUserDetailsService.DEFAULT_COMPANY,
            inboxId,
            MemberUserDetailsService.getCurrentMemberId()
        );
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
        return getUnreadInboxCount(
            MemberUserDetailsService.DEFAULT_COMPANY,
            MemberUserDetailsService.getCurrentMemberId()
        );
    }

    public long getUnreadInboxCount(String companyId, String memberId) {
        return inboxRepository.countByIdCompanyIdAndMemberIdAndIsRead(companyId, memberId, "N");
    }

    public long countInboxByType(String inboxType) {
        return countInboxByType(
            MemberUserDetailsService.DEFAULT_COMPANY,
            MemberUserDetailsService.getCurrentMemberId(),
            inboxType
        );
    }

    public long countInboxByType(String companyId, String memberId, String inboxType) {
        return inboxRepository.countByIdCompanyIdAndMemberIdAndInboxType(companyId, memberId, inboxType);
    }

    @Transactional(readOnly = true)
    public Page<ApprovalInboxResponse> getMyInbox(String type, Pageable pageable) {
        return getMyInbox(
            MemberUserDetailsService.DEFAULT_COMPANY,
            MemberUserDetailsService.getCurrentMemberId(),
            type,
            pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<ApprovalInboxResponse> getMyInbox(String companyId, String memberId, String type, Pageable pageable) {
        Page<ApprovalInbox> page;
        if (type == null || type.isBlank()) {
            page = inboxRepository.findByIdCompanyIdAndMemberId(companyId, memberId, pageable);
        } else {
            page = inboxRepository.findByIdCompanyIdAndMemberIdAndInboxType(companyId, memberId, type, pageable);
        }
        return page.map(ApprovalInboxResponse::from);
    }

    @Transactional(readOnly = true)
    public Optional<ApprovalInboxResponse> getMyInboxByApproval(String approvalId) {
        return getMyInboxByApproval(
            MemberUserDetailsService.DEFAULT_COMPANY,
            approvalId,
            MemberUserDetailsService.getCurrentMemberId()
        );
    }

    @Transactional(readOnly = true)
    public Optional<ApprovalInboxResponse> getMyInboxByApproval(String companyId, String approvalId, String memberId) {
        return inboxRepository
            .findByIdCompanyIdAndApprovalIdAndMemberId(companyId, approvalId, memberId)
            .map(ApprovalInboxResponse::from);
    }

    // ===== 내부 로직 =====

    private Approval getExisting(String approvalId) {
        return repository
            .findByIdCompanyIdAndIdApprovalId(MemberUserDetailsService.DEFAULT_COMPANY, approvalId)
            .orElseThrow(() -> new NotFoundException("Approval not found: " + approvalId));
    }

    private ApprovalResponse toResponseWithoutSteps(Approval approval) {
        return ApprovalResponse.from(approval, Collections.emptyList());
    }

    private String normalizeIdempotencyKey(String rawKey) {
        if (rawKey == null) {
            return null;
        }
        return rawKey.trim().toUpperCase();
    }

    private void validateIdempotencyKey(String companyId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey는 필수입니다.");
        }
        if (!IDEMPOTENCY_KEY_PATTERN.matcher(idempotencyKey).matches()) {
            throw new IllegalArgumentException("멱등키 형식이 올바르지 않습니다. 입력값: " + idempotencyKey);
        }
        if (!idempotencyKey.startsWith(companyId + "_")) {
            throw new IllegalArgumentException("멱등키 회사 ID가 일치하지 않습니다.");
        }
    }

    private List<ApprovalStep> persistSteps(
        Approval approval,
        List<ApprovalStepRequest> stepRequests,
        LocalDateTime createdAt,
        String submittedBy
    ) {
        if (stepRequests == null || stepRequests.isEmpty()) {
            return Collections.emptyList();
        }

        String companyId = approval.getCompanyId();
        String approvalId = approval.getApprovalId();
        List<ApprovalStep> steps = new ArrayList<>();

        for (int i = 0; i < stepRequests.size(); i++) {
            ApprovalStepRequest request = stepRequests.get(i);
            int stepNo = request.stepNo() != null && request.stepNo() > 0 ? request.stepNo() : i + 1;

            ApprovalStep step = new ApprovalStep();
            step.setId(new ApprovalStepId(companyId, approvalId, stepNo));
            step.setMemberId(request.memberId());
            step.setDecision(request.decision());
            step.setResult(null);
            step.setDecidedAt(null);
            step.setComment(null);
            ApprovalStep savedStep = stepRepository.save(step);
            steps.add(savedStep);

            createInboxEntry(approval, savedStep, createdAt, submittedBy);
        }

        return steps;
    }

    private void createInboxEntry(
        Approval approval,
        ApprovalStep step,
        LocalDateTime createdAt,
        String submittedBy
    ) {
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
        inbox.setSubmittedBy(submittedBy);
        inbox.setSubmittedAt(approval.getSubmittedAt());
        inbox.setCreatedAt(createdAt);
        inboxRepository.save(inbox);
    }

    private ApprovalResponse processApproval(
        String approvalId,
        String comment,
        String finalStatusWhenCompleted,
        ApprovalEventType eventType
    ) {
        Approval approval = getExisting(approvalId);
        LocalDateTime now = LocalDateTime.now();
        String currentMemberId = MemberUserDetailsService.getCurrentMemberId();

        if (!STATUS_SUBMITTED.equals(approval.getStatus()) && !STATUS_IN_PROGRESS.equals(approval.getStatus())) {
            throw new IllegalStateException("결재 대기 중인 문서만 처리할 수 있습니다. 현재 상태: " + approval.getStatus());
        }

        List<ApprovalStep> steps = stepRepository
            .findByIdCompanyIdAndIdApprovalIdOrderByIdStepNo(approval.getCompanyId(), approvalId);

        ApprovalStep targetStep = steps.stream()
            .filter(step -> currentMemberId.equals(step.getMemberId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("결재 권한이 없습니다."));

        if (targetStep.getDecidedAt() != null) {
            throw new IllegalStateException("이미 결재한 문서입니다.");
        }

        enforceStepOrder(steps, targetStep);

        targetStep.setDecidedAt(now);
        targetStep.setComment(comment);
        targetStep.setResult(ApprovalEventType.REJECTED.equals(eventType) ? STATUS_REJECTED : STATUS_APPROVED);
        stepRepository.save(targetStep);

        boolean isReject = ApprovalEventType.REJECTED.equals(eventType) && !DECISION_INFO.equals(targetStep.getDecision());
        boolean allCompleted = checkAllApproversCompleted(steps);

        if (isReject) {
            approval.setStatus(STATUS_REJECTED);
            approval.setCompletedAt(now);
        } else if (allCompleted) {
            approval.setStatus(finalStatusWhenCompleted);
            approval.setCompletedAt(now);
        } else {
            approval.setStatus(STATUS_IN_PROGRESS);
            approval.setCompletedAt(null);
        }

        approval.setUpdatedAt(now);
        approval.setUpdatedBy(currentMemberId);
        Approval saved = repository.save(approval);

        updateInboxAfterDecision(saved, targetStep, now);

        if (isReject || allCompleted) {
            enqueueOutbox(saved, steps, eventType, now, currentMemberId, comment);
        } else {
            enqueueOutbox(saved, steps, ApprovalEventType.SUBMITTED, now, currentMemberId, null);
        }

        List<ApprovalStepResponse> stepResponses = steps.stream()
            .map(ApprovalStepResponse::from)
            .collect(Collectors.toList());
        return ApprovalResponse.from(saved, stepResponses);
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
                throw new IllegalStateException("이전 결재자가 먼저 결재해야 합니다.");
            }
            if (STATUS_REJECTED.equals(step.getResult())) {
                throw new IllegalStateException("이전 결재자가 반려하여 결재가 종료되었습니다.");
            }
        }
    }

    private boolean checkAllApproversCompleted(List<ApprovalStep> steps) {
        return steps.stream()
            .filter(step -> isSequentialDecision(step.getDecision()))
            .allMatch(step -> step.getDecidedAt() != null && STATUS_APPROVED.equals(step.getResult()));
    }

    private boolean isSequentialDecision(String decision) {
        return DECISION_APPROVAL.equals(decision) || DECISION_AGREE.equals(decision);
    }

    private void updateInboxAfterDecision(
        Approval approval,
        ApprovalStep step,
        LocalDateTime decidedAt
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
        } else if (STATUS_REJECTED.equals(step.getResult())) {
            inbox.setInboxType(INBOX_REJECTED);
        } else {
            inbox.setInboxType(INBOX_APPROVED);
        }

        inbox.setIsRead("Y");
        inbox.setReadAt(decidedAt);
        inbox.setUpdatedAt(decidedAt);
        inboxRepository.save(inbox);
    }

    private void updateInboxAfterCancel(Approval approval, LocalDateTime cancelledAt) {
        List<ApprovalInbox> inboxes = inboxRepository
            .findByIdCompanyIdAndApprovalId(approval.getCompanyId(), approval.getApprovalId());

        for (ApprovalInbox inbox : inboxes) {
            inbox.setInboxType(INBOX_COMPLETED);
            inbox.setIsRead("Y");
            inbox.setReadAt(cancelledAt);
            inbox.setUpdatedAt(cancelledAt);
            inboxRepository.save(inbox);
        }
    }

    private void enqueueOutbox(
        Approval approval,
        List<ApprovalStep> steps,
        ApprovalEventType eventType,
        LocalDateTime occurredAt,
        String actorId,
        String comment
    ) {
        ApprovalEventPayload payload = new ApprovalEventPayload(
            approval.getCompanyId(),
            approval.getApprovalId(),
            approval.getRefEntity(),
            approval.getRefId(),
            approval.getRefStage(),
            approval.getStatus(),
            eventType,
            occurredAt,
            actorId,
            comment,
            approval.getCallbackUrl(),
            approval.getIdempotencyKey(),
            steps.stream()
                .map(step -> new ApprovalEventPayload.ApprovalEventStep(
                    step.getId() != null ? step.getId().getStepNo() : null,
                    step.getMemberId(),
                    step.getDecision(),
                    step.getResult(),
                    step.getDecidedAt(),
                    step.getComment()
                ))
                .collect(Collectors.toList())
        );

        String payloadJson = toJson(payload);

        ApprovalOutbox outbox = new ApprovalOutbox();
        outbox.setCompanyId(approval.getCompanyId());
        outbox.setApprovalId(approval.getApprovalId());
        outbox.setCallbackUrl(approval.getCallbackUrl());
        outbox.setIdempotencyKey(approval.getIdempotencyKey());
        outbox.setEventType(eventType);
        outbox.setStatus(ApprovalOutboxStatus.PENDING);
        outbox.setPayload(payloadJson);
        outbox.setRetryCount(0);
        outbox.setCreatedAt(occurredAt);
        outbox.setUpdatedAt(occurredAt);
        outbox.setNextAttemptAt(occurredAt);

        outboxRepository.save(outbox);
        log.debug(
            "Outbox 이벤트 등록 - approvalId={}, eventType={}, outboxId={}",
            approval.getApprovalId(),
            eventType,
            outbox.getId()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Outbox 페이로드 직렬화에 실패했습니다.", e);
        }
    }
}
