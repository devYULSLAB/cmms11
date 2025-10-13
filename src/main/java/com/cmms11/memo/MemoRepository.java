package com.cmms11.memo;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 이름: MemoRepository
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 메모 엔티티의 CRUD 및 검색을 위한 JPA 레포지토리.
 */
public interface MemoRepository extends JpaRepository<Memo, MemoId> {

    Page<Memo> findByIdCompanyId(String companyId, Pageable pageable);
    Page<Memo> findByIdCompanyIdAndPlantId(String companyId, String plantId, Pageable pageable);

    @Query(
        "select m from Memo m " +
        "where m.id.companyId = :companyId and (m.id.memoId like :keyword or m.title like :keyword or m.content like :keyword)"
    )
    Page<Memo> search(
        @Param("companyId") String companyId,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Query(
        "select m from Memo m " +
        "where m.id.companyId = :companyId " +
        "and (:title is null or :title = '' or m.title like concat('%', :title, '%')) " +
        "and (:createdBy is null or :createdBy = '' or m.createdBy like concat('%', :createdBy, '%')) " +
        "and (:refEntity is null or :refEntity = '' or m.refEntity like concat('%', :refEntity, '%')) " +
        "and (:status is null or :status = '' or m.status = :status) " +
        "and (:stage is null or :stage = '' or m.stage = :stage)"
    )
    Page<Memo> findByFilters(
        @Param("companyId") String companyId,
        @Param("title") String title,
        @Param("createdBy") String createdBy,
        @Param("refEntity") String refEntity,
        @Param("status") String status,
        @Param("stage") String stage,
        Pageable pageable
    );

    Optional<Memo> findByIdCompanyIdAndIdMemoId(String companyId, String memoId);
}
