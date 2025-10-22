package com.cmms11.file;

import com.cmms11.common.error.NotFoundException;
import com.cmms11.common.seq.AutoNumberService;
import com.cmms11.file.storage.StorageService;
import com.cmms11.security.MemberUserDetailsService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class FileService {

    private static final String MODULE_CODE = "F";

    private final FileGroupRepository groupRepository;
    private final FileItemRepository itemRepository;
    private final AutoNumberService autoNumberService;
    private final StorageService storageService;
    private final long maxFileSize;
    private final Set<String> allowedExtensions;

    public FileService(
        FileGroupRepository groupRepository,
        FileItemRepository itemRepository,
        AutoNumberService autoNumberService,
        StorageService storageService,
        @Value("${app.file-storage.max-size:10485760}") long maxFileSize,
        @Value("${app.file-storage.allowed-extensions:jpg,jpeg,png,pdf,txt}") String allowedExtensions
    ) {
        this.groupRepository = groupRepository;
        this.itemRepository = itemRepository;
        this.autoNumberService = autoNumberService;
        this.storageService = storageService;
        this.maxFileSize = maxFileSize;
        this.allowedExtensions = Arrays.stream(allowedExtensions.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
    }

    public FileGroupResponse upload(String requestedGroupId, String refEntity, String refId, List<MultipartFile> files) {
        System.out.println("파일 업로드 시작 - 요청된 그룹ID: " + requestedGroupId + ", 파일 수: " + (files != null ? files.size() : 0));
        
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 존재하지 않습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        String memberId = MemberUserDetailsService.getCurrentMemberId();

        FileGroup group = resolveGroup(requestedGroupId, refEntity, refId, companyId, memberId, now);
        group.setUpdatedAt(now);
        group.setUpdatedBy(memberId);
        group = groupRepository.save(group);

        // ✅ DB 쿼리 제거: maxLineNo 조회 대신 메모리에서 카운팅
        int currentLineNo = 0;

        for (MultipartFile file : files) {
            System.out.println("파일 처리 시작 - 원본명: " + file.getOriginalFilename() + ", 크기: " + file.getSize() + ", 타입: " + file.getContentType());
            
            if (file.isEmpty()) {
                throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
            }
            String originalName = cleanFileName(file.getOriginalFilename());
            String extension = extractExtension(originalName);
            validateExtension(extension, originalName);
            validateSize(file.getSize(), originalName);

            // ✅ UUID 기반 fileId 생성 (sequence LOCK 제거!)
            String fileId = generateShortFileId();
            String storedName = buildStoredName(fileId, extension);
            
            // 파일 저장 (StorageService 사용)
            String storagePath;
            try (InputStream inputStream = file.getInputStream()) {
                storagePath = storageService.store(
                    companyId, 
                    group.getId().getFileGroupId(), 
                    storedName,
                    inputStream, 
                    file.getContentType()
                );
            } catch (IOException e) {
                throw new IllegalStateException("파일을 저장할 수 없습니다.", e);
            }

            FileItem item = new FileItem();
            item.setId(new FileItemId(companyId, group.getId().getFileGroupId(), fileId));
            item.setLineNo(++currentLineNo);
            item.setOriginalName(originalName);
            item.setStoredName(storedName);
            item.setExt(extension);
            item.setMime(file.getContentType());
            item.setSize(file.getSize());
            item.setChecksumSha256(null);  // checksum 제거
            item.setStoragePath(storagePath);
            item.setDeleteMark("N");
            item.setCreatedAt(now);
            item.setCreatedBy(memberId);
            item.setUpdatedAt(now);
            item.setUpdatedBy(memberId);
            itemRepository.save(item);
        }

        return toResponse(group, activeItems(companyId, group.getId().getFileGroupId()));
    }

    @Transactional(readOnly = true)
    public FileGroupResponse getGroup(String groupId) {
        if (!StringUtils.hasText(groupId)) {
            throw new IllegalArgumentException("fileGroupId 는 필수입니다.");
        }
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        FileGroup group = groupRepository
            .findByIdCompanyIdAndIdFileGroupId(companyId, groupId)
            .orElseThrow(() -> new NotFoundException("파일 그룹을 찾을 수 없습니다: " + groupId));
        return toResponse(group, activeItems(companyId, groupId));
    }

    @Transactional(readOnly = true)
    public FileDownload download(String groupId, String fileId) {
        FileItem item = requireActiveFile(groupId, fileId);
        String storagePath = item.getStoragePath();
        
        try {
            // 파일 존재 확인
            if (!storageService.exists(storagePath)) {
                // Fallback: 구 경로 시도
                if (storageService.existsLegacy(groupId, item.getStoredName())) {
                    InputStream inputStream = storageService.retrieveLegacy(groupId, item.getStoredName());
                    return createFileDownload(inputStream, item);
                }
                throw new NotFoundException("파일을 찾을 수 없습니다: " + fileId);
            }
            
            // InputStream 가져오기
            InputStream inputStream = storageService.retrieve(storagePath);
            return createFileDownload(inputStream, item);
            
        } catch (Exception e) {
            throw new IllegalStateException("파일 다운로드 실패: " + fileId, e);
        }
    }
    
    private FileDownload createFileDownload(InputStream inputStream, FileItem item) {
        Resource resource = new InputStreamResource(inputStream) {
            @Override
            public String getFilename() {
                return item.getOriginalName();
            }
        };
        
        return new FileDownload(
            resource,
            item.getOriginalName(),
            item.getMime(),
            item.getSize()
        );
    }

    public void delete(String groupId, String fileId) {
        FileItem item = requireActiveFile(groupId, fileId);
        
        // ⭐ 소프트 삭제: 물리 파일은 유지, DB에 deleteMark = 'Y' 설정
        LocalDateTime now = LocalDateTime.now();
        String memberId = MemberUserDetailsService.getCurrentMemberId();
        
        item.setDeleteMark("Y");
        item.setUpdatedAt(now);
        item.setUpdatedBy(memberId);
        itemRepository.save(item);

        // 그룹 업데이트
        FileGroup group = groupRepository
            .findByIdCompanyIdAndIdFileGroupId(MemberUserDetailsService.DEFAULT_COMPANY, groupId)
            .orElseThrow(() -> new NotFoundException("파일 그룹을 찾을 수 없습니다: " + groupId));
        group.setUpdatedAt(now);
        group.setUpdatedBy(memberId);
        groupRepository.save(group);
        
        // ⚠️ 물리 파일은 90일 후 별도 배치 작업으로 삭제
        // TODO: 스케줄러 작업 추가 필요 (예: FileCleanupScheduler)
    }

    private FileGroup resolveGroup(
        String requestedGroupId,
        String refEntity,
        String refId,
        String companyId,
        String memberId,
        LocalDateTime now
    ) {
        if (StringUtils.hasText(requestedGroupId)) {
            FileGroup existing = groupRepository
                .findByIdCompanyIdAndIdFileGroupId(companyId, requestedGroupId)
                .orElseThrow(() -> new NotFoundException("파일 그룹을 찾을 수 없습니다: " + requestedGroupId));
            if (StringUtils.hasText(refEntity)) {
                existing.setRefEntity(refEntity);
            }
            if (StringUtils.hasText(refId)) {
                existing.setRefId(refId);
            }
            return existing;
        }
        FileGroup group = new FileGroup();
        String newGroupId = autoNumberService.generateTxId(companyId, MODULE_CODE, LocalDate.now());
        group.setId(new FileGroupId(companyId, newGroupId));
        group.setRefEntity(refEntity);
        group.setRefId(refId);
        group.setDeleteMark("N");
        group.setCreatedAt(now);
        group.setCreatedBy(memberId);
        return group;
    }

    private List<FileItemResponse> activeItems(String companyId, String groupId) {
        return itemRepository
            .findByIdCompanyIdAndIdFileGroupIdAndDeleteMark(companyId, groupId, "N")
            .stream()
            .sorted(Comparator.comparing(FileItem::getLineNo))
            .map(FileItemResponse::from)
            .toList();
    }

    private FileGroupResponse toResponse(FileGroup group, List<FileItemResponse> items) {
        return new FileGroupResponse(group.getId().getFileGroupId(), group.getRefEntity(), group.getRefId(), items);
    }

    private FileItem requireActiveFile(String groupId, String fileId) {
        if (!StringUtils.hasText(groupId)) {
            throw new IllegalArgumentException("fileGroupId 는 필수입니다.");
        }
        if (!StringUtils.hasText(fileId)) {
            throw new IllegalArgumentException("fileId 는 필수입니다.");
        }
        return itemRepository
            .findByIdCompanyIdAndIdFileGroupIdAndIdFileIdAndDeleteMark(
                MemberUserDetailsService.DEFAULT_COMPANY,
                groupId,
                fileId,
                "N"
            )
            .orElseThrow(() -> new NotFoundException("파일을 찾을 수 없습니다: " + fileId));
    }

    private String cleanFileName(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            throw new IllegalArgumentException("파일 이름이 비어 있습니다.");
        }
        return Paths.get(originalName).getFileName().toString();
    }

    private String extractExtension(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            return "";
        }
        
        String extension = StringUtils.getFilenameExtension(originalName);
        if (extension == null) {
            return "";
        }
        
        String lowerExtension = extension.toLowerCase();
        System.out.println("파일명: " + originalName + ", 추출된 확장자: " + lowerExtension);
        return lowerExtension;
    }

    private void validateExtension(String extension, String originalName) {
        System.out.println("확장자 검증 - 파일명: " + originalName + ", 확장자: " + extension + ", 허용된 확장자: " + allowedExtensions);
        
        if (!allowedExtensions.isEmpty() && !allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않은 확장자입니다: " + originalName + " (확장자: " + extension + ")");
        }
    }

    private void validateSize(long size, String originalName) {
        if (size <= 0) {
            throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다: " + originalName);
        }
        if (maxFileSize > 0 && size > maxFileSize) {
            throw new IllegalArgumentException("파일 크기가 허용 범위를 초과했습니다: " + originalName);
        }
    }

    private String buildStoredName(String fileId, String extension) {
        if (!StringUtils.hasText(extension)) {
            return fileId;
        }
        return fileId + "." + extension;
    }


    /**
     * 짧은 UUID 기반 fileId 생성 (sequence 사용 안 함)
     * @return 10자리 고유 ID
     */
    private String generateShortFileId() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}
