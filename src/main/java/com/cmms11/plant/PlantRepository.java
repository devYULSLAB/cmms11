package com.cmms11.plant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlantRepository extends JpaRepository<Plant, PlantId> {
    Page<Plant> findByIdCompanyIdAndDeleteMark(String companyId, String deleteMark, Pageable pageable);

    @Query("select p from Plant p where p.id.companyId=:companyId and p.deleteMark=:deleteMark and (p.id.plantId like :q or p.name like :q)")
    Page<Plant> search(@Param("companyId") String companyId,
                       @Param("deleteMark") String deleteMark,
                       @Param("q") String q,
                       Pageable pageable);
    
    @Query(
        "select p from Plant p " +
        "where p.id.companyId = :companyId " +
        "and p.deleteMark = :deleteMark " +
        "and (:plantId is null or :plantId = '' or p.id.plantId like concat('%', :plantId, '%')) " +
        "and (:name is null or :name = '' or p.name like concat('%', :name, '%')) " +
        "and (:makerName is null or :makerName = '' or p.makerName like concat('%', :makerName, '%')) " +
        "and (:funcId is null or :funcId = '' or p.funcId like concat('%', :funcId, '%'))"
    )
    Page<Plant> findByFilters(
        @Param("companyId") String companyId,
        @Param("deleteMark") String deleteMark,
        @Param("plantId") String plantId,
        @Param("name") String name,
        @Param("makerName") String makerName,
        @Param("funcId") String funcId,
        Pageable pageable
    );
}

