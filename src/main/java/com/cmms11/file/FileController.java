package com.cmms11.file;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileGroupResponse> upload(
        @RequestParam(value = "groupId", required = false) String groupId,
        @RequestParam(value = "refEntity", required = false) String refEntity,
        @RequestParam(value = "refId", required = false) String refId,
        @RequestParam("files") List<MultipartFile> files
    ) {
        try {
            System.out.println("FileController.upload 호출됨 - 그룹ID: " + groupId + ", 파일 수: " + files.size());
            FileGroupResponse response = fileService.upload(groupId, refEntity, refId, files);
            System.out.println("파일 업로드 성공 - 응답 그룹ID: " + response.fileGroupId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            System.err.println("파일 업로드 오류: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<FileGroupResponse> getGroup(@RequestParam("groupId") String groupId) {
        return ResponseEntity.ok(fileService.getGroup(groupId));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> download(
        @PathVariable String fileId,
        @RequestParam("groupId") String groupId
    ) {
        FileDownload download = fileService.download(groupId, fileId);
        String contentType = download.mimeType();
        if (!StringUtils.hasText(contentType)) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        String encodedName = UriUtils.encode(download.originalName(), StandardCharsets.UTF_8);
        // 안전한 ASCII 파일명 생성 (확장자 유지)
        String safeFilename = "file_" + fileId + getFileExtension(download.originalName());
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + safeFilename + "\"; filename*=UTF-8''" + encodedName
            )
            .contentLength(download.size())
            .body(download.resource());
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(@PathVariable String fileId, @RequestParam("groupId") String groupId) {
        fileService.delete(groupId, fileId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 파일명에서 확장자를 추출하는 헬퍼 메서드
     * @param filename 원본 파일명
     * @return 확장자 (점 포함, 없으면 빈 문자열)
     */
    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot);
    }
}
