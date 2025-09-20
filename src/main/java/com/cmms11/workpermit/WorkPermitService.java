package com.cmms11.workpermit;

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
 * 이름: WorkPermitService
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 작업허가 트랜잭션 CRUD 로직을 처리하는 서비스.
 */
@Service
@Transactional
public class WorkPermitService {

    private static final String MODULE_CODE = "P";

    private final WorkPermitRepository repository;
    private final AutoNumberService autoNumberService;

    public WorkPermitService(WorkPermitRepository repository, AutoNumberService autoNumberService) {
        this.repository = repository;
        this.autoNumberService = autoNumberService;
    }

    @Transactional(readOnly = true)
    public Page<WorkPermitResponse> list(String keyword, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Page<WorkPermit> page;
        if (keyword == null || keyword.isBlank()) {
            page = repository.findByIdCompanyId(companyId, pageable);
        } else {
            String trimmed = "%" + keyword.trim() + "%";
            page = repository.search(companyId, trimmed, pageable);
        }
        return page.map(WorkPermitResponse::from);
    }

    @Transactional(readOnly = true)
    public WorkPermitResponse get(String workPermitId) {
        return WorkPermitResponse.from(getExisting(workPermitId));
    }

    public WorkPermitResponse create(WorkPermitRequest request) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        LocalDateTime now = LocalDateTime.now();
        String memberId = currentMemberId();

        String newId = resolveId(companyId, request.workPermitId(), request.plannedDate());
        WorkPermit entity = new WorkPermit();
        entity.setId(new WorkPermitId(companyId, newId));
        entity.setCreatedAt(now);
        entity.setCreatedBy(memberId);
        applyRequest(entity, request);
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(memberId);

        return WorkPermitResponse.from(repository.save(entity));
    }

    public WorkPermitResponse update(String workPermitId, WorkPermitRequest request) {
        WorkPermit entity = getExisting(workPermitId);
        applyRequest(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentMemberId());
        return WorkPermitResponse.from(repository.save(entity));
    }

    public void delete(String workPermitId) {
        WorkPermit entity = getExisting(workPermitId);
        repository.delete(entity);
    }

    private WorkPermit getExisting(String workPermitId) {
        return repository
            .findByIdCompanyIdAndIdWorkPermitId(MemberUserDetailsService.DEFAULT_COMPANY, workPermitId)
            .orElseThrow(() -> new NotFoundException("WorkPermit not found: " + workPermitId));
    }

    private void applyRequest(WorkPermit entity, WorkPermitRequest request) {
        entity.setName(request.name());
        entity.setPlantId(request.plantId());
        entity.setJobId(request.jobId());
        entity.setSiteId(request.siteId());
        entity.setDeptId(request.deptId());
        entity.setMemberId(request.memberId());
        entity.setPlannedDate(request.plannedDate());
        entity.setActualDate(request.actualDate());
        entity.setWorkSummary(request.workSummary());
        entity.setHazardFactor(request.hazardFactor());
        entity.setSafetyFactor(request.safetyFactor());
        entity.setChecksheetJson(request.checksheetJson());
        entity.setStatus(request.status());
        entity.setFileGroupId(request.fileGroupId());
        entity.setNote(request.note());
    }

    private String resolveId(String companyId, String requestedId, LocalDate referenceDate) {
        if (requestedId != null && !requestedId.isBlank()) {
            String trimmed = requestedId.trim();
            repository
                .findByIdCompanyIdAndIdWorkPermitId(companyId, trimmed)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("WorkPermit already exists: " + trimmed);
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
