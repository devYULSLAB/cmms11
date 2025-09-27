package com.cmms11.file;

import com.cmms11.common.error.NotFoundException;
import com.cmms11.common.seq.AutoNumberService;
import com.cmms11.security.MemberUserDetailsService;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class FileService {

    private static final String MODULE_CODE = "F";
    private static final String DELETE_MARK_Y = "Y";

    private final FileGroupRepository groupRepository;
    private final FileItemRepository itemRepository;
    private final AutoNumberService autoNumberService;
    private final Path storageRoot;
    private final long maxFileSize;
    private final Set<String> allowedExtensions;

    public FileService(
        FileGroupRepository groupRepository,
        FileItemRepository itemRepository,
        AutoNumberService autoNumberService,
        @Value("${app.file-storage.location:storage/uploads}") String storageLocation,
        @Value("${app.file-storage.max-size:10485760}") long maxFileSize,
        @Value("${app.file-storage.allowed-extensions:jpg,jpeg,png,pdf,txt}") String allowedExtensions
    ) {
        this.groupRepository = groupRepository;
        this.itemRepository = itemRepository;
        this.autoNumberService = autoNumberService;
        this.storageRoot = Paths.get(storageLocation).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize;
        this.allowedExtensions = Arrays.stream(allowedExtensions.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
        try {
            Files.createDirectories(this.storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장소를 초기화할 수 없습니다.", e);
        }
    }

    public FileGroupResponse upload(String requestedGroupId, String refEntity, String refId, List<MultipartFile> files) {
        System.out.println("파일 업로드 시작 - 요청된 그룹ID: " + requestedGroupId + ", 파일 수: " + (files != null ? files.size() : 0));
        
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 존재하지 않습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        String memberId = currentMemberId();

        FileGroup group = resolveGroup(requestedGroupId, refEntity, refId, companyId, memberId, now);
        group.setUpdatedAt(now);
        group.setUpdatedBy(memberId);
        group = groupRepository.save(group);

        Integer maxLineNo = itemRepository.findMaxLineNo(companyId, group.getId().getFileGroupId());
        int currentLineNo = maxLineNo != null ? maxLineNo : 0;

        for (MultipartFile file : files) {
            System.out.println("파일 처리 시작 - 원본명: " + file.getOriginalFilename() + ", 크기: " + file.getSize() + ", 타입: " + file.getContentType());
            
            if (file.isEmpty()) {
                throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
            }
            String originalName = cleanFileName(file.getOriginalFilename());
            String extension = extractExtension(originalName);
            validateExtension(extension, originalName);
            validateSize(file.getSize(), originalName);

            String fileId = autoNumberService.generateTxId(companyId, MODULE_CODE, LocalDate.now());
            String storedName = buildStoredName(fileId, extension);
            Path target = prepareTargetPath(group.getId().getFileGroupId(), storedName);
            String checksum = storeFile(file, target);

            FileItem item = new FileItem();
            item.setId(new FileItemId(companyId, group.getId().getFileGroupId(), fileId));
            item.setLineNo(++currentLineNo);
            item.setOriginalName(originalName);
            item.setStoredName(storedName);
            item.setExt(extension);
            item.setMime(file.getContentType());
            item.setSize(file.getSize());
            item.setChecksumSha256(checksum);
            item.setStoragePath(storageRoot.relativize(target).toString().replace('\\', '/'));
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
        Path filePath = storageRoot.resolve(item.getStoragePath()).normalize();
        if (!filePath.startsWith(storageRoot) || !Files.exists(filePath)) {
            throw new NotFoundException("파일을 찾을 수 없습니다: " + fileId);
        }
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("파일을 읽을 수 없습니다: " + fileId);
            }
            return new FileDownload(
                resource,
                item.getOriginalName(),
                item.getMime(),
                item.getSize() != null ? item.getSize() : resource.contentLength()
            );
        } catch (MalformedURLException e) {
            throw new IllegalStateException("파일을 읽을 수 없습니다: " + fileId, e);
        } catch (IOException e) {
            throw new IllegalStateException("파일 크기를 확인할 수 없습니다: " + fileId, e);
        }
    }

    public void delete(String groupId, String fileId) {
        FileItem item = requireActiveFile(groupId, fileId);
        Path filePath = storageRoot.resolve(item.getStoragePath()).normalize();
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new IllegalStateException("파일을 삭제할 수 없습니다: " + fileId, e);
        }

        LocalDateTime now = LocalDateTime.now();
        String memberId = currentMemberId();
        item.setDeleteMark(DELETE_MARK_Y);
        item.setUpdatedAt(now);
        item.setUpdatedBy(memberId);
        itemRepository.save(item);

        FileGroup group = groupRepository
            .findByIdCompanyIdAndIdFileGroupId(MemberUserDetailsService.DEFAULT_COMPANY, groupId)
            .orElseThrow(() -> new NotFoundException("파일 그룹을 찾을 수 없습니다: " + groupId));
        group.setUpdatedAt(now);
        group.setUpdatedBy(memberId);
        groupRepository.save(group);
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
        group.setDeleteMark("N");
        group.setRefEntity(refEntity);
        group.setRefId(refId);
        group.setCreatedAt(now);
        group.setCreatedBy(memberId);
        return group;
    }

    private List<FileItemResponse> activeItems(String companyId, String groupId) {
        return itemRepository
            .findByIdCompanyIdAndIdFileGroupIdAndDeleteMarkNot(companyId, groupId, DELETE_MARK_Y)
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
            .findByIdCompanyIdAndIdFileGroupIdAndIdFileIdAndDeleteMarkNot(
                MemberUserDetailsService.DEFAULT_COMPANY,
                groupId,
                fileId,
                DELETE_MARK_Y
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

    private Path prepareTargetPath(String groupId, String storedName) {
        Path groupDir = storageRoot.resolve(groupId);
        try {
            Files.createDirectories(groupDir);
        } catch (IOException e) {
            throw new IllegalStateException("파일 디렉터리를 생성할 수 없습니다.", e);
        }
        Path target = groupDir.resolve(storedName).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new IllegalArgumentException("잘못된 파일 경로가 감지되었습니다.");
        }
        return target;
    }

    private String storeFile(MultipartFile file, Path target) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream inputStream = new DigestInputStream(file.getInputStream(), digest)) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new IllegalStateException("파일을 저장할 수 없습니다.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("지원되지 않는 해시 알고리즘입니다.", e);
        }
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
