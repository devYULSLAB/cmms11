package com.cmms11.domain.site;

import com.cmms11.common.error.NotFoundException;
import com.cmms11.security.MemberUserDetailsService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이름: SiteService
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 사업장 기준정보의 CRUD 로직을 담당하는 서비스.
 */

@Service
@Transactional
public class SiteService {

    private final SiteRepository repository;

    public SiteService(SiteRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)

    public Page<SiteResponse> list(String keyword, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Page<Site> page;
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
        return page.map(SiteResponse::from);
    }

    @Transactional(readOnly = true)
    public SiteResponse get(String siteId) {
        return SiteResponse.from(getActiveSite(siteId));
    }

    public SiteResponse create(SiteRequest request) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Optional<Site> existing = repository.findByIdCompanyIdAndIdSiteId(companyId, request.siteId());
        LocalDateTime now = LocalDateTime.now();
        String memberId = currentMemberId();

        if (existing.isPresent()) {
            Site entity = existing.get();
            if (!"Y".equalsIgnoreCase(entity.getDeleteMark())) {
                throw new IllegalArgumentException("Site already exists: " + request.siteId());
            }
            entity.setName(request.name());
            entity.setPhone(request.phone());
            entity.setAddress(request.address());
            entity.setStatus(request.status());
            entity.setNote(request.note());
            entity.setDeleteMark("N");
            entity.setUpdatedAt(now);
            entity.setUpdatedBy(memberId);
            return SiteResponse.from(repository.save(entity));
        }

        Site site = new Site();
        site.setId(new SiteId(companyId, request.siteId()));
        site.setName(request.name());
        site.setPhone(request.phone());
        site.setAddress(request.address());
        site.setStatus(request.status());
        site.setNote(request.note());
        site.setDeleteMark("N");
        site.setCreatedAt(now);
        site.setCreatedBy(memberId);
        site.setUpdatedAt(now);
        site.setUpdatedBy(memberId);
        return SiteResponse.from(repository.save(site));
    }

    public SiteResponse update(String siteId, SiteRequest request) {
        Site existing = getActiveSite(siteId);
        existing.setName(request.name());
        existing.setPhone(request.phone());
        existing.setAddress(request.address());
        existing.setStatus(request.status());
        existing.setNote(request.note());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(currentMemberId());
        return SiteResponse.from(repository.save(existing));
    }

    public void delete(String siteId) {
        Site existing = getActiveSite(siteId);
        existing.setDeleteMark("Y");
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(currentMemberId());
        repository.save(existing);
    }

    private Site getActiveSite(String siteId) {
        return repository.findByIdCompanyIdAndIdSiteId(MemberUserDetailsService.DEFAULT_COMPANY, siteId)
            .filter(site -> !"Y".equalsIgnoreCase(site.getDeleteMark()))
            .orElseThrow(() -> new NotFoundException("Site not found: " + siteId));
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

