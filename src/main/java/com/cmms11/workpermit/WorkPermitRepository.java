package com.cmms11.workpermit;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 이름: WorkPermitRepository
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 작업허가 엔티티에 대한 CRUD 및 검색 JPA 레포지토리.
 */
public interface WorkPermitRepository extends JpaRepository<WorkPermit, WorkPermitId> {

    Page<WorkPermit> findByIdCompanyId(String companyId, Pageable pageable);

    @Query(
        "select w from WorkPermit w " +
        "where w.id.companyId = :companyId and (w.id.permitId like :keyword or w.name like :keyword)"
    )
    Page<WorkPermit> search(
        @Param("companyId") String companyId,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Query(
        "select w from WorkPermit w " +
        "where w.id.companyId = :companyId " +
        "and (:permitId is null or :permitId = '' or w.id.permitId like concat('%', :permitId, '%')) " +
        "and (:plantId is null or :plantId = '' or w.plantId like concat('%', :plantId, '%')) " +
        "and (:jobId is null or :jobId = '' or w.jobId = :jobId) " +
        "and (:status is null or :status = '' or w.status = :status) " +
        "and (:plannedDateFrom is null or w.plannedDate >= :plannedDateFrom)"
    )
    Page<WorkPermit> findByFilters(
        @Param("companyId") String companyId,
        @Param("permitId") String permitId,
        @Param("plantId") String plantId,
        @Param("jobId") String jobId,
        @Param("status") String status,
        @Param("plannedDateFrom") java.time.LocalDate plannedDateFrom,
        Pageable pageable
    );

    Optional<WorkPermit> findByIdCompanyIdAndIdPermitId(String companyId, String permitId);
}
