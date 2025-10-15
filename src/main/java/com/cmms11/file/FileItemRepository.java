package com.cmms11.file;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileItemRepository extends JpaRepository<FileItem, FileItemId> {

    List<FileItem> findByIdCompanyIdAndIdFileGroupId(String companyId, String fileGroupId);

    Optional<FileItem> findByIdCompanyIdAndIdFileGroupIdAndIdFileId(
        String companyId,
        String fileGroupId,
        String fileId
    );

    // 소프트 삭제 지원: deleteMark = 'N'인 파일만 조회
    List<FileItem> findByIdCompanyIdAndIdFileGroupIdAndDeleteMark(String companyId, String fileGroupId, String deleteMark);

    Optional<FileItem> findByIdCompanyIdAndIdFileGroupIdAndIdFileIdAndDeleteMark(
        String companyId,
        String fileGroupId,
        String fileId,
        String deleteMark
    );
}
