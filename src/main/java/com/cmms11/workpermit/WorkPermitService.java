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
    public Page<WorkPermitResponse> list(String permitId, String plantId, String jobId, String status, String plannedDateFrom, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        
        // 날짜 문자열을 LocalDate로 변환
        java.time.LocalDate fromDate = null;
        
        try {
            if (plannedDateFrom != null && !plannedDateFrom.isBlank()) {
                fromDate = java.time.LocalDate.parse(plannedDateFrom);
            }
        } catch (java.time.format.DateTimeParseException e) {
            // 날짜 파싱 오류 시 무시
        }
        
        Page<WorkPermit> page = repository.findByFilters(
            companyId, 
            permitId, 
            plantId, 
            jobId, 
            status, 
            fromDate, 
            pageable
        );
        
        return page.map(WorkPermitResponse::from);
    }

    @Transactional(readOnly = true)
    public WorkPermitResponse get(String permitId) {
        return WorkPermitResponse.from(getExisting(permitId));
    }

    public WorkPermitResponse create(WorkPermitRequest request) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        LocalDateTime now = LocalDateTime.now();
        String memberId = currentMemberId();

        String newId = resolveId(companyId, request.permitId(), request.plannedDate());
        WorkPermit entity = new WorkPermit();
        entity.setId(new WorkPermitId(companyId, newId));
        entity.setCreatedAt(now);
        entity.setCreatedBy(memberId);
        applyRequest(entity, request);
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(memberId);

        return WorkPermitResponse.from(repository.save(entity));
    }

    public WorkPermitResponse update(String permitId, WorkPermitRequest request) {
        WorkPermit entity = getExisting(permitId);
        applyRequest(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentMemberId());
        return WorkPermitResponse.from(repository.save(entity));
    }

    public void delete(String permitId) {
        WorkPermit entity = getExisting(permitId);
        repository.delete(entity);
    }

    private WorkPermit getExisting(String permitId) {
        return repository
            .findByIdCompanyIdAndIdPermitId(MemberUserDetailsService.DEFAULT_COMPANY, permitId)
            .orElseThrow(() -> new NotFoundException("WorkPermit not found: " + permitId));
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
                .findByIdCompanyIdAndIdPermitId(companyId, trimmed)
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
