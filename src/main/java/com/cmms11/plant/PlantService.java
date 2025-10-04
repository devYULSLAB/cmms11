package com.cmms11.plant;

import com.cmms11.common.error.NotFoundException;
import com.cmms11.common.seq.AutoNumberService;
import com.cmms11.common.upload.BulkUploadError;
import com.cmms11.common.upload.BulkUploadResult;
import com.cmms11.common.upload.CsvUtils;
import com.cmms11.security.MemberUserDetailsService;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

@Service
@Transactional
public class PlantService {
    private static final String MODULE_CODE = "1";

    private final PlantRepository repository;
    private final AutoNumberService autoNumberService;

    public PlantService(PlantRepository repository, AutoNumberService autoNumberService) {
        this.repository = repository;
        this.autoNumberService = autoNumberService;
    }

    @Transactional(readOnly = true)
    public Page<PlantResponse> list(String plantId, String name, String makerName, String funcId, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Page<Plant> page = repository.findByFilters(
            companyId,
            "N",
            plantId,
            name,
            makerName,
            funcId,
            pageable
        );
        return page.map(PlantResponse::from);
    }

    @Transactional(readOnly = true)
    public PlantResponse get(String plantId) {
        Plant plant = getActivePlant(plantId);
        return PlantResponse.from(plant);
    }

    public PlantResponse create(PlantRequest request) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        LocalDateTime now = LocalDateTime.now();
        String memberId = currentMemberId();

        // plantId가 없으면 자동 생성
        String plantId = request.plantId();
        if (plantId == null || plantId.isBlank()) {
            plantId = autoNumberService.generateMasterId(companyId, MODULE_CODE);
        }

        Plant plant = new Plant();
        plant.setId(new PlantId(companyId, plantId));
        plant.setName(request.name());
        plant.setAssetId(request.assetId());
        plant.setSiteId(request.siteId());
        plant.setDeptId(request.deptId());
        plant.setFuncId(request.funcId());
        plant.setMakerName(request.makerName());
        plant.setSpec(request.spec());
        plant.setModel(request.model());
        plant.setSerial(request.serial());
        plant.setInstallDate(request.installDate());
        plant.setDepreId(request.depreId());
        plant.setDeprePeriod(request.deprePeriod());
        plant.setPurchaseCost(request.purchaseCost());
        plant.setResidualValue(request.residualValue());
        plant.setInspectionYn(request.inspectionYn());
        plant.setPsmYn(request.psmYn());
        plant.setWorkpermitYn(request.workpermitYn());
        plant.setInspectionInterval(request.inspectionInterval());
        plant.setLastInspection(request.lastInspection());
        plant.setNextInspection(request.nextInspection());
        plant.setFileGroupId(request.fileGroupId());
        plant.setNote(request.note());
        plant.setStatus(request.status());
        plant.setDeleteMark("N");
        plant.setCreatedAt(now);
        plant.setCreatedBy(memberId);
        plant.setUpdatedAt(now);
        plant.setUpdatedBy(memberId);

        return PlantResponse.from(repository.save(plant));
    }

    public PlantResponse update(String plantId, PlantRequest request) {
        Plant existing = getActivePlant(plantId);
        
        existing.setName(request.name());
        existing.setAssetId(request.assetId());
        existing.setSiteId(request.siteId());
        existing.setDeptId(request.deptId());
        existing.setFuncId(request.funcId());
        existing.setMakerName(request.makerName());
        existing.setSpec(request.spec());
        existing.setModel(request.model());
        existing.setSerial(request.serial());
        existing.setInstallDate(request.installDate());
        existing.setDepreId(request.depreId());
        existing.setDeprePeriod(request.deprePeriod());
        existing.setPurchaseCost(request.purchaseCost());
        existing.setResidualValue(request.residualValue());
        existing.setInspectionYn(request.inspectionYn());
        existing.setPsmYn(request.psmYn());
        existing.setWorkpermitYn(request.workpermitYn());
        existing.setInspectionInterval(request.inspectionInterval());
        existing.setLastInspection(request.lastInspection());
        existing.setNextInspection(request.nextInspection());
        existing.setFileGroupId(request.fileGroupId());
        existing.setNote(request.note());
        existing.setStatus(request.status());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(currentMemberId());

        return PlantResponse.from(repository.save(existing));
    }

    private Plant getActivePlant(String plantId) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        PlantId id = new PlantId(companyId, plantId);
        return repository.findById(id)
            .filter(plant -> !"Y".equalsIgnoreCase(plant.getDeleteMark()))
            .orElseThrow(() -> new NotFoundException("Plant not found: " + plantId));
    }

    public void delete(String plantId) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        PlantId id = new PlantId(companyId, plantId);

        // 기존 Plant 존재 확인
        Plant existing = repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Plant not found: " + plantId));

        // 소프트 삭제 (delete_mark = 'Y')
        existing.setDeleteMark("Y");
        repository.save(existing);
    }

    public BulkUploadResult upload(MultipartFile file) {
        List<BulkUploadError> errors = new ArrayList<>();
        List<Plant> pending = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        LocalDateTime now = LocalDateTime.now();
        String memberId = currentMemberId();

        try (CSVParser parser = CsvUtils.parse(file)) {
            Map<String, Integer> headerIndex = CsvUtils.normalizeHeaderMap(parser);
            CsvUtils.requireHeaders(headerIndex, List.of("name"));

            for (CSVRecord record : parser) {
                if (CsvUtils.isEmptyRecord(record)) {
                    continue;
                }
                int rowNumber = CsvUtils.displayRowNumber(record);
                try {
                    Plant plant = mapPlantRecord(record, headerIndex, companyId, now, memberId, seenIds);
                    pending.add(plant);
                    seenIds.add(plant.getId().getPlantId());
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

    private Plant mapPlantRecord(
        CSVRecord record,
        Map<String, Integer> headerIndex,
        String companyId,
        LocalDateTime now,
        String memberId,
        Set<String> seenIds
    ) {
        String csvPlantId = CsvUtils.getString(record, headerIndex, "plant_id");
        if (csvPlantId != null && seenIds.contains(csvPlantId)) {
            throw new IllegalArgumentException("CSV 내에서 중복된 설비 ID입니다: " + csvPlantId);
        }

        Plant plant;
        String finalPlantId;
        if (csvPlantId == null) {
            finalPlantId = autoNumberService.generateMasterId(companyId, MODULE_CODE);
            plant = new Plant();
            plant.setId(new PlantId(companyId, finalPlantId));
            plant.setCreatedAt(now);
            plant.setCreatedBy(memberId);
        } else {
            finalPlantId = csvPlantId;
            PlantId id = new PlantId(companyId, finalPlantId);
            Optional<Plant> existing = repository.findById(id);
            if (existing.isPresent()) {
                plant = existing.get();
                if (!"Y".equalsIgnoreCase(plant.getDeleteMark())) {
                    throw new IllegalArgumentException("이미 존재하는 설비 ID입니다: " + finalPlantId);
                }
                if (plant.getCreatedAt() == null) {
                    plant.setCreatedAt(now);
                }
                if (plant.getCreatedBy() == null) {
                    plant.setCreatedBy(memberId);
                }
            } else {
                plant = new Plant();
                plant.setId(id);
                plant.setCreatedAt(now);
                plant.setCreatedBy(memberId);
            }
        }

        plant.setName(CsvUtils.requireNonBlank(CsvUtils.getString(record, headerIndex, "name"), "name"));
        plant.setAssetId(CsvUtils.getString(record, headerIndex, "asset_id"));
        plant.setSiteId(CsvUtils.getString(record, headerIndex, "site_id"));
        plant.setDeptId(CsvUtils.getString(record, headerIndex, "dept_id"));
        plant.setFuncId(CsvUtils.getString(record, headerIndex, "func_id"));
        plant.setMakerName(CsvUtils.getString(record, headerIndex, "maker_name"));
        plant.setSpec(CsvUtils.getString(record, headerIndex, "spec"));
        plant.setModel(CsvUtils.getString(record, headerIndex, "model"));
        plant.setSerial(CsvUtils.getString(record, headerIndex, "serial"));
        plant.setInstallDate(toDate(record, headerIndex, "install_date"));
        plant.setDepreId(CsvUtils.getString(record, headerIndex, "depre_id"));
        plant.setDeprePeriod(toInteger(record, headerIndex, "depre_period"));
        plant.setPurchaseCost(toBigDecimal(record, headerIndex, "purchase_cost"));
        plant.setResidualValue(toBigDecimal(record, headerIndex, "residual_value"));
        plant.setInspectionYn(normalizeFlag(record, headerIndex, "inspection_yn"));
        plant.setPsmYn(normalizeFlag(record, headerIndex, "psm_yn"));
        plant.setWorkpermitYn(normalizeFlag(record, headerIndex, "workpermit_yn"));
        plant.setInspectionInterval(toInteger(record, headerIndex, "inspection_interval"));
        plant.setLastInspection(toDate(record, headerIndex, "last_inspection"));
        plant.setNextInspection(toDate(record, headerIndex, "next_inspection"));
        plant.setFileGroupId(CsvUtils.getString(record, headerIndex, "file_group_id"));
        plant.setNote(CsvUtils.getString(record, headerIndex, "note"));
        plant.setStatus(CsvUtils.getString(record, headerIndex, "status"));
        plant.setDeleteMark("N");
        plant.setUpdatedAt(now);
        plant.setUpdatedBy(memberId);
        return plant;
    }

    private Integer toInteger(CSVRecord record, Map<String, Integer> headerIndex, String column) {
        return CsvUtils.toInteger(CsvUtils.getString(record, headerIndex, column), column);
    }

    private BigDecimal toBigDecimal(CSVRecord record, Map<String, Integer> headerIndex, String column) {
        return CsvUtils.toBigDecimal(CsvUtils.getString(record, headerIndex, column), column);
    }

    private LocalDate toDate(CSVRecord record, Map<String, Integer> headerIndex, String column) {
        return CsvUtils.toLocalDate(CsvUtils.getString(record, headerIndex, column), column);
    }

    private String normalizeFlag(CSVRecord record, Map<String, Integer> headerIndex, String column) {
        String value = CsvUtils.getString(record, headerIndex, column);
        if (value == null) {
            return null;
        }
        String upper = value.trim().toUpperCase(Locale.ROOT);
        if (!upper.equals("Y") && !upper.equals("N")) {
            throw new IllegalArgumentException(column + " 값은 Y 또는 N 이어야 합니다: " + value);
        }
        return upper;
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
