package com.cmms11.memo;

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
 * 이름: MemoService
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 메모 CRUD 로직을 제공하는 서비스.
 */
@Service
@Transactional
public class MemoService {

    private static final String MODULE_CODE = "M";

    private final MemoRepository repository;
    private final AutoNumberService autoNumberService;

    public MemoService(MemoRepository repository, AutoNumberService autoNumberService) {
        this.repository = repository;
        this.autoNumberService = autoNumberService;
    }

    @Transactional(readOnly = true)
    public Page<MemoResponse> list(String keyword, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Page<Memo> page;
        if (keyword == null || keyword.isBlank()) {
            page = repository.findByIdCompanyId(companyId, pageable);
        } else {
            String trimmed = "%" + keyword.trim() + "%";
            page = repository.search(companyId, trimmed, pageable);
        }
        return page.map(MemoResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<MemoResponse> list(String title, String createdBy, String refEntity, String status, String stage, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        
        Page<Memo> page = repository.findByFilters(
            companyId, 
            title, 
            createdBy, 
            refEntity, 
            status,
            stage,
            pageable
        );
        
        return page.map(MemoResponse::from);
    }

    @Transactional(readOnly = true)
    public MemoResponse get(String memoId) {
        return MemoResponse.from(getExisting(memoId));
    }

    public MemoResponse create(MemoRequest request) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        LocalDateTime now = LocalDateTime.now();
        String memberId = currentMemberId();

        String newId = resolveId(companyId, request.memoId());
        Memo entity = new Memo();
        entity.setId(new MemoId(companyId, newId));
        entity.setCreatedAt(now);
        entity.setCreatedBy(memberId);
        applyRequest(entity, request);
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(memberId);

        return MemoResponse.from(repository.save(entity));
    }

    public MemoResponse update(String memoId, MemoRequest request) {
        Memo entity = getExisting(memoId);
        applyRequest(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentMemberId());
        return MemoResponse.from(repository.save(entity));
    }

    public void delete(String memoId) {
        Memo entity = getExisting(memoId);
        repository.delete(entity);
    }

    private Memo getExisting(String memoId) {
        return repository
            .findByIdCompanyIdAndIdMemoId(MemberUserDetailsService.DEFAULT_COMPANY, memoId)
            .orElseThrow(() -> new NotFoundException("Memo not found: " + memoId));
    }

    private void applyRequest(Memo entity, MemoRequest request) {
        entity.setTitle(request.title());
        entity.setContent(request.content());
        entity.setRefEntity(request.refEntity());
        entity.setRefId(request.refId());
        entity.setFileGroupId(request.fileGroupId());
    }

    private String resolveId(String companyId, String requestedId) {
        if (requestedId != null && !requestedId.isBlank()) {
            String trimmed = requestedId.trim();
            repository
                .findByIdCompanyIdAndIdMemoId(companyId, trimmed)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Memo already exists: " + trimmed);
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
