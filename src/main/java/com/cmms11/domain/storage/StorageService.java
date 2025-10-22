package com.cmms11.domain.storage;

import com.cmms11.common.error.NotFoundException;
import com.cmms11.security.MemberUserDetailsService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이름: StorageService
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 창고 기준정보 CRUD 서비스.
 */
@Service
@Transactional
public class StorageService {

    private final StorageRepository repository;

    public StorageService(StorageRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)

    public Page<StorageResponse> list(String keyword, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Page<Storage> page;
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
        return page.map(StorageResponse::from);
    }

    @Transactional(readOnly = true)
    public StorageResponse get(String storageId) {
        return StorageResponse.from(getActiveStorage(storageId));
    }

    public StorageResponse create(StorageRequest request) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Optional<Storage> existing = repository.findByIdCompanyIdAndIdStorageId(companyId, request.storageId());
        LocalDateTime now = LocalDateTime.now();
        String memberId = currentMemberId();

        if (existing.isPresent()) {
            Storage storage = existing.get();
            if (!"Y".equalsIgnoreCase(storage.getDeleteMark())) {
                throw new IllegalArgumentException("Storage already exists: " + request.storageId());
            }
            storage.setName(request.name());
            storage.setNote(request.note());
            storage.setDeleteMark("N");
            storage.setUpdatedAt(now);
            storage.setUpdatedBy(memberId);
            return StorageResponse.from(repository.save(storage));
        }

        Storage storage = new Storage();
        storage.setId(new StorageId(companyId, request.storageId()));
        storage.setName(request.name());
        storage.setNote(request.note());
        storage.setDeleteMark("N");
        storage.setCreatedAt(now);
        storage.setCreatedBy(memberId);
        storage.setUpdatedAt(now);
        storage.setUpdatedBy(memberId);
        return StorageResponse.from(repository.save(storage));
    }

    public StorageResponse update(String storageId, StorageRequest request) {
        Storage existing = getActiveStorage(storageId);
        existing.setName(request.name());
        existing.setNote(request.note());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(currentMemberId());
        return StorageResponse.from(repository.save(existing));
    }

    public void delete(String storageId) {
        Storage existing = getActiveStorage(storageId);
        existing.setDeleteMark("Y");
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(currentMemberId());
        repository.save(existing);
    }

    private Storage getActiveStorage(String storageId) {
        return repository.findByIdCompanyIdAndIdStorageId(MemberUserDetailsService.DEFAULT_COMPANY, storageId)
            .filter(storage -> !"Y".equalsIgnoreCase(storage.getDeleteMark()))
            .orElseThrow(() -> new NotFoundException("Storage not found: " + storageId));
    }

    private String currentMemberId() {
        return MemberUserDetailsService.getCurrentMemberId();
    }
}

