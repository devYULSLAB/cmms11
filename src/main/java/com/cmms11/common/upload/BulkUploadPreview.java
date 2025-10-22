package com.cmms11.common.upload;

import java.util.List;

/**
 * CSV 업로드 미리보기 결과
 * 검증된 데이터와 오류 목록을 포함
 */
public record BulkUploadPreview<T>(
    List<T> validItems,
    List<BulkUploadError> errors
) {
    public int successCount() {
        return validItems.size();
    }
    
    public int failureCount() {
        return errors.size();
    }
}

