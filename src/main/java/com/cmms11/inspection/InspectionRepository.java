package com.cmms11.inspection;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 이름: InspectionRepository
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: 예방점검 엔티티 CRUD 및 검색 기능을 제공하는 JPA 레포지토리.
 */
public interface InspectionRepository extends JpaRepository<Inspection, InspectionId> {

    Page<Inspection> findByIdCompanyId(String companyId, Pageable pageable);

    @Query(
        "select i from Inspection i " +
        "where i.id.companyId = :companyId and (i.id.inspectionId like :keyword or i.name like :keyword)"
    )
    Page<Inspection> search(
        @Param("companyId") String companyId,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Query(
        "select i from Inspection i " +
        "where i.id.companyId = :companyId " +
        "and (:inspectionId is null or :inspectionId = '' or i.id.inspectionId like concat('%', :inspectionId, '%')) " +
        "and (:plantId is null or :plantId = '' or i.plantId like concat('%', :plantId, '%')) " +
        "and (:status is null or :status = '' or i.status = :status) " +
        "and (:plannedDateFrom is null or i.plannedDate >= :plannedDateFrom) " +
        "and (:plannedDateTo is null or i.plannedDate <= :plannedDateTo)"
    )
    Page<Inspection> findByFilters(
        @Param("companyId") String companyId,
        @Param("inspectionId") String inspectionId,
        @Param("plantId") String plantId,
        @Param("status") String status,
        @Param("plannedDateFrom") java.time.LocalDate plannedDateFrom,
        @Param("plannedDateTo") java.time.LocalDate plannedDateTo,
        Pageable pageable
    );

    Optional<Inspection> findByIdCompanyIdAndIdInspectionId(String companyId, String inspectionId);
}
