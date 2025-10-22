package com.cmms11.domain.func;

import com.cmms11.common.error.NotFoundException;
import com.cmms11.security.MemberUserDetailsService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이름: FuncService
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 기능위치 CRUD 비즈니스 로직 구현.
 */
@Service
@Transactional
public class FuncService {

    private final FuncRepository repository;

    public FuncService(FuncRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)

    public Page<FuncResponse> list(String keyword, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Page<Func> page;
        if (keyword == null || keyword.isBlank()) {
            page = repository.findByIdCompanyIdAndDeleteMark(companyId, "N", pageable);
        } else {
            page = repository.findByIdCompanyIdAndDeleteMarkAndNameContainingIgnoreCase(
                companyId,
                "N",
                keyword.trim(),
                pageable
            );
        }
        return page.map(FuncResponse::from);
    }

    @Transactional(readOnly = true)
    public FuncResponse get(String funcId) {
        return FuncResponse.from(getActiveFunc(funcId));
    }

    public FuncResponse create(FuncRequest request) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Optional<Func> existing = repository.findByIdCompanyIdAndIdFuncId(companyId, request.funcId());
        LocalDateTime now = LocalDateTime.now();
        String memberId = MemberUserDetailsService.getCurrentMemberId();

        if (existing.isPresent()) {
            Func func = existing.get();
            if (!"Y".equalsIgnoreCase(func.getDeleteMark())) {
                throw new IllegalArgumentException("Func already exists: " + request.funcId());
            }
            func.setName(request.name());
            func.setNote(request.note());
            func.setDeleteMark("N");
            func.setUpdatedAt(now);
            func.setUpdatedBy(memberId);
            return FuncResponse.from(repository.save(func));
        }

        Func func = new Func();
        func.setId(new FuncId(companyId, request.funcId()));
        func.setName(request.name());
        func.setNote(request.note());
        func.setDeleteMark("N");
        func.setCreatedAt(now);
        func.setCreatedBy(memberId);
        func.setUpdatedAt(now);
        func.setUpdatedBy(memberId);
        return FuncResponse.from(repository.save(func));
    }

    public FuncResponse update(String funcId, FuncRequest request) {
        Func existing = getActiveFunc(funcId);
        existing.setName(request.name());
        existing.setNote(request.note());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(MemberUserDetailsService.getCurrentMemberId());
        return FuncResponse.from(repository.save(existing));
    }

    public void delete(String funcId) {
        Func existing = getActiveFunc(funcId);
        existing.setDeleteMark("Y");
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(MemberUserDetailsService.getCurrentMemberId());
        repository.save(existing);
    }

    private Func getActiveFunc(String funcId) {
        return repository.findByIdCompanyIdAndIdFuncId(MemberUserDetailsService.DEFAULT_COMPANY, funcId)
            .filter(func -> !"Y".equalsIgnoreCase(func.getDeleteMark()))
            .orElseThrow(() -> new NotFoundException("Func not found: " + funcId));
    }
}
