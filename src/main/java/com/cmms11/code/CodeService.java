package com.cmms11.code;

import com.cmms11.common.error.NotFoundException;
import com.cmms11.security.MemberUserDetailsService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이름: CodeService
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 공통 코드 타입/항목 CRUD 비즈니스 로직 구현.
 */
@Service
@Transactional
public class CodeService {

    private final CodeTypeRepository typeRepository;
    private final CodeItemRepository itemRepository;

    public CodeService(CodeTypeRepository typeRepository, CodeItemRepository itemRepository) {
        this.typeRepository = typeRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public Page<CodeTypeResponse> listTypes(String keyword, Pageable pageable) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Page<CodeType> page;
        if (keyword == null || keyword.isBlank()) {
            page = typeRepository.findByIdCompanyIdAndDeleteMark(companyId, "N", pageable);
        } else {
            page = typeRepository.findByIdCompanyIdAndDeleteMarkAndNameContainingIgnoreCase(
                companyId,
                "N",
                keyword.trim(),
                pageable
            );
        }
        return page.map(CodeTypeResponse::from);
    }

    @Transactional(readOnly = true)
    public CodeTypeResponse getType(String codeType) {
        return CodeTypeResponse.from(getActiveType(codeType));
    }

    public CodeTypeResponse createType(CodeTypeRequest request) {
        String companyId = MemberUserDetailsService.DEFAULT_COMPANY;
        Optional<CodeType> existing = typeRepository.findByIdCompanyIdAndIdCodeType(companyId, request.codeType());
        LocalDateTime now = LocalDateTime.now();
        String memberId = MemberUserDetailsService.getCurrentMemberId();

        if (existing.isPresent()) {
            CodeType type = existing.get();
            if (!"Y".equalsIgnoreCase(type.getDeleteMark())) {
                throw new IllegalArgumentException("Code type already exists: " + request.codeType());
            }
            type.setName(request.name());
            type.setNote(request.note());
            type.setDeleteMark("N");
            type.setUpdatedAt(now);
            type.setUpdatedBy(memberId);
            return CodeTypeResponse.from(typeRepository.save(type));
        }

        CodeType type = new CodeType();
        type.setId(new CodeTypeId(companyId, request.codeType()));
        type.setName(request.name());
        type.setNote(request.note());
        type.setDeleteMark("N");
        type.setCreatedAt(now);
        type.setCreatedBy(memberId);
        type.setUpdatedAt(now);
        type.setUpdatedBy(memberId);
        return CodeTypeResponse.from(typeRepository.save(type));
    }

    public CodeTypeResponse updateType(String codeType, CodeTypeRequest request) {
        CodeType type = getActiveType(codeType);
        type.setName(request.name());
        type.setNote(request.note());
        type.setUpdatedAt(LocalDateTime.now());
        type.setUpdatedBy(MemberUserDetailsService.getCurrentMemberId());
        return CodeTypeResponse.from(typeRepository.save(type));
    }

    public void deleteType(String codeType) {
        CodeType type = getActiveType(codeType);
        List<CodeItem> items = itemRepository.findByIdCompanyIdAndIdCodeType(type.getId().getCompanyId(), codeType);
        if (!items.isEmpty()) {
            throw new IllegalStateException("Cannot delete code type with existing items: " + codeType);
        }
        type.setDeleteMark("Y");
        type.setUpdatedAt(LocalDateTime.now());
        type.setUpdatedBy(MemberUserDetailsService.getCurrentMemberId());
        typeRepository.save(type);
    }

    @Transactional(readOnly = true)
    public Page<CodeItemResponse> listItems(String codeType, String keyword, Pageable pageable) {
        CodeType type = getActiveType(codeType);
        Page<CodeItem> page;
        if (keyword == null || keyword.isBlank()) {
            page = itemRepository.findByIdCompanyIdAndIdCodeType(type.getId().getCompanyId(), codeType, pageable);
        } else {
            page = itemRepository.findByIdCompanyIdAndIdCodeTypeAndNameContainingIgnoreCase(
                type.getId().getCompanyId(),
                codeType,
                keyword.trim(),
                pageable
            );
        }
        return page.map(CodeItemResponse::from);
    }

    @Transactional(readOnly = true)
    public CodeItemResponse getItem(String codeType, String code) {
        return CodeItemResponse.from(getItemEntity(codeType, code));
    }

    public CodeItemResponse createItem(CodeItemRequest request) {
        CodeType type = getActiveType(request.codeType());
        Optional<CodeItem> existing = itemRepository.findByIdCompanyIdAndIdCodeTypeAndIdCode(
            type.getId().getCompanyId(),
            request.codeType(),
            request.code()
        );
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Code item already exists: " + request.code());
        }
        CodeItem item = new CodeItem();
        item.setId(new CodeItemId(type.getId().getCompanyId(), request.codeType(), request.code()));
        item.setName(request.name());
        item.setNote(request.note());
        return CodeItemResponse.from(itemRepository.save(item));
    }

    public CodeItemResponse updateItem(String codeType, String code, CodeItemRequest request) {
        CodeItem item = getItemEntity(codeType, code);
        item.setName(request.name());
        item.setNote(request.note());
        return CodeItemResponse.from(itemRepository.save(item));
    }

    public void deleteItem(String codeType, String code) {
        CodeItem item = getItemEntity(codeType, code);
        itemRepository.delete(item);
    }

    private CodeType getActiveType(String codeType) {
        return typeRepository.findByIdCompanyIdAndIdCodeType(MemberUserDetailsService.DEFAULT_COMPANY, codeType)
            .filter(type -> !"Y".equalsIgnoreCase(type.getDeleteMark()))
            .orElseThrow(() -> new NotFoundException("Code type not found: " + codeType));
    }

    private CodeItem getItemEntity(String codeType, String code) {
        return itemRepository.findByIdCompanyIdAndIdCodeTypeAndIdCode(
                MemberUserDetailsService.DEFAULT_COMPANY,
                codeType,
                code
            )
            .orElseThrow(() -> new NotFoundException("Code item not found: " + codeType + "/" + code));
    }

}

