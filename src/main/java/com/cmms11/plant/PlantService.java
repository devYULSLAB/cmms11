package com.cmms11.plant;

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
        String memberId = MemberUserDetailsService.getCurrentMemberId();

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
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(MemberUserDetailsService.getCurrentMemberId());

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

    /**
     * CSV 파일 검증 (저장하지 않음)
     * 기본정보 + 제조사 정보만 업로드
     * @return 유효한 데이터 리스트와 오류 내역
     */
    @Transactional(readOnly = true)
    public BulkUploadPreview<PlantUploadDto> validateUpload(MultipartFile file) {
        List<PlantUploadDto> validItems = new ArrayList<>();
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
                    PlantUploadDto dto = validateRecord(record, headerIndex, companyId, seenIds);
                    validItems.add(dto);
                    seenIds.add(dto.plantId());
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
    private PlantUploadDto validateRecord(
        CSVRecord record,
        Map<String, Integer> headerIndex,
        String companyId,
        Set<String> seenIds
    ) {
        // plant_id 추출 및 검증
        String csvPlantId = CsvUtils.getString(record, headerIndex, "plant_id");
        
        // CSV 내 중복 체크
        if (csvPlantId != null && seenIds.contains(csvPlantId)) {
            throw new IllegalArgumentException("CSV 내에서 중복된 설비 ID: " + csvPlantId);
        }
        
        // DB 중복 체크 (기존 데이터와의 충돌)
        if (csvPlantId != null) {
            PlantId id = new PlantId(companyId, csvPlantId);
            Optional<Plant> existing = repository.findById(id);
            if (existing.isPresent() && !"Y".equalsIgnoreCase(existing.get().getDeleteMark())) {
                throw new IllegalArgumentException("이미 존재하는 설비 ID: " + csvPlantId);
            }
        }
        
        // 필수 필드 검증
        String name = CsvUtils.getString(record, headerIndex, "name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name은 필수입니다");
        }
        
        // 자동생성 ID 표시
        String displayId = csvPlantId != null ? csvPlantId : "(자동생성)";
        
        return new PlantUploadDto(
            displayId,
            name,
            CsvUtils.getString(record, headerIndex, "asset_id"),
            CsvUtils.getString(record, headerIndex, "site_id"),
            CsvUtils.getString(record, headerIndex, "dept_id"),
            CsvUtils.getString(record, headerIndex, "func_id"),
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
    public BulkUploadResult saveUploadedData(List<PlantUploadDto> items) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        LocalDateTime now = LocalDateTime.now();
        String memberId = MemberUserDetailsService.getCurrentMemberId();
        
        int successCount = 0;
        List<BulkUploadError> errors = new ArrayList<>();
        
        // 라인별로 저장 처리
        for (int i = 0; i < items.size(); i++) {
            PlantUploadDto dto = items.get(i);
            int rowNumber = i + 2; // CSV 행번호 (헤더 제외)
            
            try {
                Plant plant = convertDtoToPlant(dto, companyId, now, memberId);
                repository.save(plant);
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
     * DTO를 Plant 엔티티로 변환
     */
    private Plant convertDtoToPlant(
        PlantUploadDto dto,
        String companyId,
        LocalDateTime now,
        String memberId
    ) {
        Plant plant = new Plant();
        
        // plant_id 처리 (자동생성 또는 지정)
        String plantId;
        if (dto.plantId().equals("(자동생성)")) {
            plantId = autoNumberService.generateMasterId(companyId, MODULE_CODE);
        } else {
            plantId = dto.plantId();
        }
        
        plant.setId(new PlantId(companyId, plantId));
        
        // 기본정보
        plant.setName(dto.name());
        plant.setAssetId(dto.assetId());
        plant.setSiteId(dto.siteId());
        plant.setDeptId(dto.deptId());
        plant.setFuncId(dto.funcId());
        plant.setNote(dto.note());
        
        // 제조사 정보
        plant.setMakerName(dto.makerName());
        plant.setModel(dto.model());
        plant.setSerial(dto.serial());
        plant.setSpec(dto.spec());
        
        // 기본값 설정
        plant.setInspectionYn("N");
        plant.setPsmYn("N");
        plant.setWorkpermitYn("N");
        plant.setDeleteMark("N");
        
        // 메타 정보
        plant.setCreatedAt(now);
        plant.setCreatedBy(memberId);
        plant.setUpdatedAt(now);
        plant.setUpdatedBy(memberId);
        
        return plant;
    }

}
