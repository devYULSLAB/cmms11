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
}
