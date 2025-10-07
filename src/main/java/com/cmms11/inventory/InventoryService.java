package com.cmms11.inventory;

import com.cmms11.common.error.NotFoundException;
import com.cmms11.common.seq.AutoNumberService;
import com.cmms11.common.upload.BulkUploadError;
import com.cmms11.common.upload.BulkUploadResult;
import com.cmms11.common.upload.CsvUtils;
import com.cmms11.security.MemberUserDetailsService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이름: InventoryService
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 재고 마스터 CRUD 및 조회 로직을 담당하는 서비스.
 */
@Service
@Transactional
public class InventoryService {

    private static final String MODULE_CODE = "2"; // Inventory master per STRUCTURES.md

    private final InventoryRepository repository;
    private final AutoNumberService autoNumberService;

    public InventoryService(InventoryRepository repository, AutoNumberService autoNumberService) {
        this.repository = repository;
        this.autoNumberService = autoNumberService;
    }

    @Transactional(readOnly = true)
    public Page<InventoryResponse> list(String inventoryId, String name, String makerName, String deptId, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Page<Inventory> page = repository.findByFilters(
            companyId,
            "N",
            inventoryId,
            name,
            makerName,
            deptId,
            pageable
        );
        return page.map(InventoryResponse::from);
    }

    @Transactional(readOnly = true)
    public InventoryResponse get(String inventoryId) {
        return InventoryResponse.from(getActiveInventory(inventoryId));
    }

    public InventoryResponse create(InventoryRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Inventory entity = prepareForSave(request, now, currentMemberId());
        return InventoryResponse.from(repository.save(entity));
    }

    public InventoryResponse update(String inventoryId, InventoryRequest request) {
        Inventory entity = getActiveInventory(inventoryId);
        applyRequest(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentMemberId());
        return InventoryResponse.from(repository.save(entity));
    }

    public void delete(String inventoryId) {
        Inventory entity = getActiveInventory(inventoryId);
        entity.setDeleteMark("Y");
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentMemberId());
        repository.save(entity);
    }

    public BulkUploadResult upload(MultipartFile file) {
        List<BulkUploadError> errors = new ArrayList<>();
        List<Inventory> pending = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();
        String memberId = currentMemberId();

        try (CSVParser parser = CsvUtils.parse(file)) {
            Map<String, Integer> headerIndex = CsvUtils.normalizeHeaderMap(parser);
            CsvUtils.requireHeaders(headerIndex, List.of("name", "asset_id", "dept_id"));

            for (CSVRecord record : parser) {
                if (CsvUtils.isEmptyRecord(record)) {
                    continue;
                }
                int rowNumber = CsvUtils.displayRowNumber(record);
                try {
                    InventoryRequest request = toInventoryRequest(record, headerIndex);
                    String csvId = request.inventoryId();
                    if (csvId != null && seenIds.contains(csvId)) {
                        throw new IllegalArgumentException("CSV 내에서 중복된 자재 ID입니다: " + csvId);
                    }
                    Inventory entity = prepareForSave(request, now, memberId);
                    pending.add(entity);
                    seenIds.add(entity.getId().getInventoryId());
                } catch (IllegalArgumentException ex) {
                    errors.add(new BulkUploadError(rowNumber, ex.getMessage()));
                }
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("CSV 파일을 읽을 수 없습니다.", ex);
        }

        if (!pending.isEmpty()) {
            repository.saveAll(pending);
        }
        return new BulkUploadResult(pending.size(), errors.size(), errors);
    }

    private Inventory prepareForSave(InventoryRequest request, LocalDateTime now, String memberId) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        String requestedId = request.inventoryId();

        Inventory entity;
        if (requestedId == null || requestedId.isBlank()) {
            String generatedId = autoNumberService.generateMasterId(companyId, MODULE_CODE);
            entity = new Inventory();
            entity.setId(new InventoryId(companyId, generatedId));
            entity.setCreatedAt(now);
            entity.setCreatedBy(memberId);
        } else {
            String trimmedId = requestedId.trim();
            InventoryId id = new InventoryId(companyId, trimmedId);
            Optional<Inventory> existing = repository.findById(id);
            if (existing.isPresent()) {
                entity = existing.get();
                if (!"Y".equalsIgnoreCase(entity.getDeleteMark())) {
                    throw new IllegalArgumentException("Inventory already exists: " + trimmedId);
                }
                if (entity.getCreatedAt() == null) {
                    entity.setCreatedAt(now);
                    entity.setCreatedBy(memberId);
                }
            } else {
                entity = new Inventory();
                entity.setId(id);
                entity.setCreatedAt(now);
                entity.setCreatedBy(memberId);
            }
        }

        applyRequest(entity, request);
        entity.setDeleteMark("N");
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(memberId);
        return entity;
    }

    private InventoryRequest toInventoryRequest(CSVRecord record, Map<String, Integer> headerIndex) {
        String inventoryId = CsvUtils.getString(record, headerIndex, "inventory_id");
        String name = CsvUtils.requireNonBlank(CsvUtils.getString(record, headerIndex, "name"), "name");
        String assetId = CsvUtils.requireNonBlank(CsvUtils.getString(record, headerIndex, "asset_id"), "asset_id");
        String deptId = CsvUtils.requireNonBlank(CsvUtils.getString(record, headerIndex, "dept_id"), "dept_id");
        String makerName = CsvUtils.getString(record, headerIndex, "maker_name");
        String spec = CsvUtils.getString(record, headerIndex, "spec");
        String model = CsvUtils.getString(record, headerIndex, "model");
        String serial = CsvUtils.getString(record, headerIndex, "serial");
        String fileGroupId = CsvUtils.getString(record, headerIndex, "file_group_id");
        String note = CsvUtils.getString(record, headerIndex, "note");
        return new InventoryRequest(
            inventoryId,
            name,
            assetId,
            deptId,
            makerName,
            spec,
            model,
            serial,
            fileGroupId,
            note
        );
    }

    private Inventory getActiveInventory(String inventoryId) {
        InventoryId id = new InventoryId(MemberUserDetailsService.DEFAULT_COMPANY, inventoryId);
        return repository.findById(id)
            .filter(inventory -> !"Y".equalsIgnoreCase(inventory.getDeleteMark()))
            .orElseThrow(() -> new NotFoundException("Inventory not found: " + inventoryId));
    }

    private void applyRequest(Inventory entity, InventoryRequest request) {
        entity.setName(request.name());
        entity.setAssetId(request.assetId());
        entity.setDeptId(request.deptId());
        entity.setMakerName(request.makerName());
        entity.setSpec(request.spec());
        entity.setModel(request.model());
        entity.setSerial(request.serial());
        entity.setFileGroupId(request.fileGroupId());
        entity.setNote(request.note());
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
