package com.cmms11.approval;

import com.cmms11.common.error.NotFoundException;
import com.cmms11.common.seq.AutoNumberService;
import com.cmms11.security.MemberUserDetailsService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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

    private final ApprovalRepository repository;
    private final ApprovalStepRepository stepRepository;
    private final AutoNumberService autoNumberService;

    public ApprovalService(
        ApprovalRepository repository,
        ApprovalStepRepository stepRepository,
        AutoNumberService autoNumberService
    ) {
        this.repository = repository;
        this.stepRepository = stepRepository;
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
        return page.map(approval -> ApprovalResponse.from(approval, Collections.emptyList()));
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
        
        return page.map(approval -> ApprovalResponse.from(approval, Collections.emptyList()));
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

        replaceSteps(companyId, newId, request.steps());
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

        replaceSteps(MemberUserDetailsService.DEFAULT_COMPANY, approvalId, request.steps());
        List<ApprovalStepResponse> steps = stepRepository
            .findByIdCompanyIdAndIdApprovalIdOrderByIdStepNo(MemberUserDetailsService.DEFAULT_COMPANY, approvalId)
            .stream()
            .map(ApprovalStepResponse::from)
            .collect(Collectors.toList());
        return ApprovalResponse.from(saved, steps);
    }

    public void delete(String approvalId) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        stepRepository.deleteByIdCompanyIdAndIdApprovalId(companyId, approvalId);
        Approval entity = getExisting(approvalId);
        repository.delete(entity);
    }

    public ApprovalResponse approve(String approvalId) {
        Approval entity = getExisting(approvalId);
        LocalDateTime now = LocalDateTime.now();
        String memberId = currentMemberId();

        // 결재 상태 업데이트
        if ("SUBMIT".equals(entity.getStatus()) || "APPROVAL".equals(entity.getStatus())) {
            entity.setStatus("COMPLETE");
            entity.setCompletedAt(now);
        }
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(memberId);
        
        Approval saved = repository.save(entity);
        
        List<ApprovalStepResponse> steps = stepRepository
            .findByIdCompanyIdAndIdApprovalIdOrderByIdStepNo(MemberUserDetailsService.DEFAULT_COMPANY, approvalId)
            .stream()
            .map(ApprovalStepResponse::from)
            .collect(Collectors.toList());
        return ApprovalResponse.from(saved, steps);
    }

    public ApprovalResponse reject(String approvalId) {
        Approval entity = getExisting(approvalId);
        LocalDateTime now = LocalDateTime.now();
        String memberId = currentMemberId();

        // 반려 상태 업데이트
        entity.setStatus("REJECT");
        entity.setCompletedAt(now);
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(memberId);
        
        Approval saved = repository.save(entity);
        
        List<ApprovalStepResponse> steps = stepRepository
            .findByIdCompanyIdAndIdApprovalIdOrderByIdStepNo(MemberUserDetailsService.DEFAULT_COMPANY, approvalId)
            .stream()
            .map(ApprovalStepResponse::from)
            .collect(Collectors.toList());
        return ApprovalResponse.from(saved, steps);
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
        entity.setContent(request.content());
        entity.setFileGroupId(request.fileGroupId());
    }

    private void replaceSteps(String companyId, String approvalId, List<ApprovalStepRequest> steps) {
        stepRepository.deleteByIdCompanyIdAndIdApprovalId(companyId, approvalId);
        if (steps == null || steps.isEmpty()) {
            return;
        }
        for (int i = 0; i < steps.size(); i++) {
            ApprovalStepRequest request = steps.get(i);
            int stepNo = request.stepNo() != null && request.stepNo() > 0 ? request.stepNo() : (i + 1);
            ApprovalStep step = new ApprovalStep();
            step.setId(new ApprovalStepId(companyId, approvalId, stepNo));
            step.setMemberId(request.memberId());
            step.setDecision(request.decision());
            step.setDecidedAt(request.decidedAt());
            step.setComment(request.comment());
            stepRepository.save(step);
        }
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
}
