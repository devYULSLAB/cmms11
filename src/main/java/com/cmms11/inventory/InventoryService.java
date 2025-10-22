package com.cmms11.inventory;

import com.cmms11.common.error.NotFoundException;
import com.cmms11.common.seq.AutoNumberService;
import com.cmms11.common.upload.BulkUploadError;
import com.cmms11.common.upload.BulkUploadPreview;
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
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        String inventoryId = autoNumberService.generateMasterId(companyId, MODULE_CODE);
        
        Inventory entity = new Inventory();
        entity.setId(new InventoryId(companyId, inventoryId));
        
        applyRequest(entity, request);
        
        entity.setDeleteMark("N");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy(MemberUserDetailsService.getCurrentMemberId());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(MemberUserDetailsService.getCurrentMemberId());
        
        return InventoryResponse.from(repository.save(entity));
    }

    public InventoryResponse update(String inventoryId, InventoryRequest request) {
        Inventory entity = getActiveInventory(inventoryId);
        applyRequest(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(MemberUserDetailsService.getCurrentMemberId());
        return InventoryResponse.from(repository.save(entity));
    }

    public void delete(String inventoryId) {
        Inventory entity = getActiveInventory(inventoryId);
        entity.setDeleteMark("Y");
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(MemberUserDetailsService.getCurrentMemberId());
        repository.save(entity);
    }

    /**
     * CSV 파일 검증 (저장하지 않음)
     * 기본정보 + 제조사 정보만 업로드
     * @return 유효한 데이터 리스트와 오류 내역
     */
    @Transactional(readOnly = true)
    public BulkUploadPreview<InventoryUploadDto> validateUpload(MultipartFile file) {
        List<InventoryUploadDto> validItems = new ArrayList<>();
        List<BulkUploadError> errors = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        
        try (CSVParser parser = CsvUtils.parse(file)) {
            Map<String, Integer> headerIndex = CsvUtils.normalizeHeaderMap(parser);
            
            // 필수 컬럼 체크
            CsvUtils.requireHeaders(headerIndex, List.of("name"));
            
            for (CSVRecord record : parser) {
                if (CsvUtils.isEmptyRecord(record)) {
                    continue;
                }
                
                int rowNumber = CsvUtils.displayRowNumber(record);
                
                try {
                    InventoryUploadDto dto = validateRecord(record, headerIndex, companyId, seenIds);
                    validItems.add(dto);
                    seenIds.add(dto.inventoryId());
                } catch (IllegalArgumentException ex) {
                    errors.add(new BulkUploadError(rowNumber, ex.getMessage()));
                }
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("CSV 파일을 읽을 수 없습니다.", ex);
        }
        
        return new BulkUploadPreview<>(validItems, errors);
    }

    /**
     * CSV 레코드 검증 (기본정보 + 제조사 정보만)
     */
    private InventoryUploadDto validateRecord(
        CSVRecord record,
        Map<String, Integer> headerIndex,
        String companyId,
        Set<String> seenIds
    ) {
        // inventory_id 추출 및 검증
        String csvInventoryId = CsvUtils.getString(record, headerIndex, "inventory_id");
        
        // CSV 내 중복 체크
        if (csvInventoryId != null && seenIds.contains(csvInventoryId)) {
            throw new IllegalArgumentException("CSV 내에서 중복된 자재 ID: " + csvInventoryId);
        }
        
        // DB 중복 체크 (기존 데이터와의 충돌)
        if (csvInventoryId != null) {
            InventoryId id = new InventoryId(companyId, csvInventoryId);
            Optional<Inventory> existing = repository.findById(id);
            if (existing.isPresent() && !"Y".equalsIgnoreCase(existing.get().getDeleteMark())) {
                throw new IllegalArgumentException("이미 존재하는 자재 ID: " + csvInventoryId);
            }
        }
        
        // 필수 필드 검증
        String name = CsvUtils.getString(record, headerIndex, "name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name은 필수입니다");
        }
        
        // 자동생성 ID 표시
        String displayId = csvInventoryId != null ? csvInventoryId : "(자동생성)";
        
        return new InventoryUploadDto(
            displayId,
            name,
            CsvUtils.getString(record, headerIndex, "unit"),
            CsvUtils.getString(record, headerIndex, "maker_name"),
            CsvUtils.getString(record, headerIndex, "model"),
            CsvUtils.getString(record, headerIndex, "serial"),
            CsvUtils.getString(record, headerIndex, "spec"),
            CsvUtils.getString(record, headerIndex, "note")
        );
    }

    /**
     * 검증된 데이터 일괄 저장 (라인별 처리)
     */
    public BulkUploadResult saveUploadedData(List<InventoryUploadDto> items) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        LocalDateTime now = LocalDateTime.now();
        String memberId = MemberUserDetailsService.getCurrentMemberId();
        
        int successCount = 0;
        List<BulkUploadError> errors = new ArrayList<>();
        
        // 라인별로 저장 처리
        for (int i = 0; i < items.size(); i++) {
            InventoryUploadDto dto = items.get(i);
            int rowNumber = i + 2; // CSV 행번호 (헤더 제외)
            
            try {
                Inventory inventory = convertDtoToInventory(dto, companyId, now, memberId);
                repository.save(inventory);
                successCount++;
            } catch (Exception ex) {
                errors.add(new BulkUploadError(
                    rowNumber, 
                    "저장 실패: " + ex.getMessage()
                ));
            }
        }
        
        return new BulkUploadResult(successCount, errors.size(), errors);
    }

    /**
     * DTO를 Inventory 엔티티로 변환
     */
    private Inventory convertDtoToInventory(
        InventoryUploadDto dto,
        String companyId,
        LocalDateTime now,
        String memberId
    ) {
        Inventory inventory = new Inventory();
        
        // inventory_id 처리 (자동생성 또는 지정)
        String inventoryId;
        if (dto.inventoryId().equals("(자동생성)")) {
            inventoryId = autoNumberService.generateMasterId(companyId, MODULE_CODE);
        } else {
            inventoryId = dto.inventoryId();
        }
        
        inventory.setId(new InventoryId(companyId, inventoryId));
        
        // 기본정보
        inventory.setName(dto.name());
        inventory.setUnit(dto.unit());
        inventory.setNote(dto.note());
        
        // 제조사 정보
        inventory.setMakerName(dto.makerName());
        inventory.setModel(dto.model());
        inventory.setSerial(dto.serial());
        inventory.setSpec(dto.spec());
        
        // 기본값 설정 (필수 필드는 임시값)
        inventory.setAssetId("MAT");  // 기본 자산유형
        inventory.setDeptId("GEN");   // 기본 부서
        inventory.setDeleteMark("N");
        
        // 메타 정보
        inventory.setCreatedAt(now);
        inventory.setCreatedBy(memberId);
        inventory.setUpdatedAt(now);
        inventory.setUpdatedBy(memberId);
        
        return inventory;
    }

    private Inventory getActiveInventory(String inventoryId) {
        InventoryId id = new InventoryId(MemberUserDetailsService.DEFAULT_COMPANY, inventoryId);
        return repository.findById(id)
            .filter(inventory -> !"Y".equalsIgnoreCase(inventory.getDeleteMark()))
            .orElseThrow(() -> new NotFoundException("Inventory not found: " + inventoryId));
    }

    private void applyRequest(Inventory entity, InventoryRequest request) {
        entity.setName(request.name());
        entity.setUnit(request.unit());
        entity.setAssetId(request.assetId());
        entity.setDeptId(request.deptId());
        entity.setMakerName(request.makerName());
        entity.setSpec(request.spec());
        entity.setModel(request.model());
        entity.setSerial(request.serial());
        entity.setFileGroupId(request.fileGroupId());
        entity.setNote(request.note());
    }

}
