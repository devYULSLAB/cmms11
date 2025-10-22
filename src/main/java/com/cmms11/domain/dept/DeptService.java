package com.cmms11.domain.dept;

import com.cmms11.common.error.NotFoundException;
import com.cmms11.security.MemberUserDetailsService;
import java.time.LocalDateTime;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이름: DeptService
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 부서 기준정보의 CRUD 비즈니스 로직 처리.
 */

@Service
@Transactional
public class DeptService {


    private final DeptRepository repository;

    public DeptService(DeptRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<DeptResponse> list(String keyword, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Page<Dept> page;
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
        return page.map(DeptResponse::from);
    }

    @Transactional(readOnly = true)
    public DeptResponse get(String deptId) {
        return DeptResponse.from(getActiveDept(deptId));
    }

    public DeptResponse create(DeptRequest request) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Optional<Dept> existing = repository.findByIdCompanyIdAndIdDeptId(companyId, request.deptId());
        LocalDateTime now = LocalDateTime.now();
        String memberId = MemberUserDetailsService.getCurrentMemberId();

        if (existing.isPresent()) {
            Dept dept = existing.get();
            if (!"Y".equalsIgnoreCase(dept.getDeleteMark())) {
                throw new IllegalArgumentException("Dept already exists: " + request.deptId());
            }
            dept.setName(request.name());
            dept.setPhone(request.phone());
            dept.setAddress(request.address());
            dept.setParentId(request.parentDeptId());
            dept.setNote(request.note());
            dept.setDeleteMark("N");
            dept.setUpdatedAt(now);
            dept.setUpdatedBy(memberId);
            return DeptResponse.from(repository.save(dept));
        }

        Dept dept = new Dept();
        dept.setId(new DeptId(companyId, request.deptId()));
        dept.setName(request.name());
        dept.setPhone(request.phone());
        dept.setAddress(request.address());
        dept.setParentId(request.parentDeptId());
        dept.setNote(request.note());
        dept.setDeleteMark("N");
        dept.setCreatedAt(now);
        dept.setCreatedBy(memberId);
        dept.setUpdatedAt(now);
        dept.setUpdatedBy(memberId);
        return DeptResponse.from(repository.save(dept));
    }

    public DeptResponse update(String deptId, DeptRequest request) {
        Dept existing = getActiveDept(deptId);
        existing.setName(request.name());
        existing.setPhone(request.phone());
        existing.setAddress(request.address());
        existing.setParentId(request.parentDeptId());
        existing.setNote(request.note());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(MemberUserDetailsService.getCurrentMemberId());
        return DeptResponse.from(repository.save(existing));
    }

    public void delete(String deptId) {
        Dept existing = getActiveDept(deptId);
        existing.setDeleteMark("Y");
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(MemberUserDetailsService.getCurrentMemberId());
        repository.save(existing);
    }

    private Dept getActiveDept(String deptId) {
        return repository.findByIdCompanyIdAndIdDeptId(MemberUserDetailsService.DEFAULT_COMPANY, deptId)
            .filter(dept -> !"Y".equalsIgnoreCase(dept.getDeleteMark()))
            .orElseThrow(() -> new NotFoundException("Dept not found: " + deptId));
    }
}
