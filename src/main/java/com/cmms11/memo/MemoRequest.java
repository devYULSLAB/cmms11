package com.cmms11.memo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 이름: MemoRequest
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 메모 생성/수정 요청 DTO.
 */
public record MemoRequest(
    @Size(max = 10) String memoId,
    @Size(max = 100) String title,
    String content,
    @Size(max = 64) String refEntity,
    @Size(max = 10) String refId,
    @Size(max = 10) String fileGroupId
) {
}
