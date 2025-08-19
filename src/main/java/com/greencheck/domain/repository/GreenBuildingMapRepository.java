package com.greencheck.domain.repository;

import com.greencheck.domain.GreenBuildingMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

public interface GreenBuildingMapRepository extends JpaRepository<GreenBuildingMap, Long> {

    interface MarkerRow {
        Long getId();
        BigDecimal getLat();
        BigDecimal getLng();
        Double getDistance();
    }

    Optional<GreenBuildingMap> findFirstByNameAndAddress(String name, String address);

    @Query(value = """
        SELECT 
          gb.building_id AS id,
          gb.latitude     AS lat,
          gb.longitude    AS lng,
          (6371000 * ACOS(LEAST(1.0,
             COS(RADIANS(:lat)) * COS(RADIANS(gb.latitude)) * COS(RADIANS(gb.longitude) - RADIANS(:lng)) +
             SIN(RADIANS(:lat)) * SIN(RADIANS(gb.latitude))
          ))) AS distance
        FROM green_building_map gb
        WHERE gb.latitude IS NOT NULL AND gb.longitude IS NOT NULL
          AND (:q IS NULL OR gb.name LIKE CONCAT('%', :q, '%') OR gb.address LIKE CONCAT('%', :q, '%'))
          AND (:gradeCode IS NULL OR gb.grade_code   = :gradeCode)
          AND (:useCategory IS NULL OR gb.use_category = :useCategory)
          AND (:certYear IS NULL OR gb.cert_year     = :certYear)
        HAVING distance <= :radius
        ORDER BY distance ASC
        """,
            countQuery = """
        SELECT COUNT(*) FROM (
          SELECT 
            (6371000 * ACOS(LEAST(1.0,
               COS(RADIANS(:lat)) * COS(RADIANS(gb.latitude)) * COS(RADIANS(gb.longitude) - RADIANS(:lng)) +
               SIN(RADIANS(:lat)) * SIN(RADIANS(gb.latitude))
            ))) AS distance
          FROM green_building_map gb
          WHERE gb.latitude IS NOT NULL AND gb.longitude IS NOT NULL
            AND (:q IS NULL OR gb.name LIKE CONCAT('%', :q, '%') OR gb.address LIKE CONCAT('%', :q, '%'))
            AND (:gradeCode IS NULL OR gb.grade_code   = :gradeCode)
            AND (:useCategory IS NULL OR gb.use_category = :useCategory)
            AND (:certYear IS NULL OR gb.cert_year     = :certYear)
          HAVING distance <= :radius
        ) t
        """,
            nativeQuery = true)
    Page<MarkerRow> searchMarkers(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radius") int radiusMeters,
            @Param("q") String q,
            @Param("gradeCode") String gradeCode,
            @Param("useCategory") String useCategory,
            @Param("certYear") Integer certYear,
            Pageable pageable
    );

    //등급 distinct
    @Query("select distinct g.gradeCode from GreenBuildingMap g " +
            "where g.gradeCode is not null and g.gradeCode <> '' " +
            "order by g.gradeCode asc")
    List<String> findDistinctGradeCodes();

    //용도 distinct
    @Query("select distinct g.useCategory from GreenBuildingMap g " +
            "where g.useCategory is not null and g.useCategory <> '' " +
            "order by g.useCategory asc")
    List<String> findDistinctUseCategories();

    //연도 distinct (내림차순)
    @Query("select distinct g.certYear from GreenBuildingMap g " +
            "where g.certYear is not null " +
            "order by g.certYear desc")
    List<Integer> findDistinctCertYears();


    //검색부분
    interface SearchRow {
        Long getId();
        String getName();
        String getAddress();
        BigDecimal getLat();
        BigDecimal getLng();
    }

    // 이름/주소 LIKE 검색 (좌표가 있는 데이터만)
    @Query("""
        select g.id as id, g.name as name, g.address as address, g.latitude as lat, g.longitude as lng
        from GreenBuildingMap g
        where ( :q is null or :q = '' 
                or lower(g.name) like lower(concat('%', :q, '%'))
                or lower(g.address) like lower(concat('%', :q, '%')) )
          and g.latitude is not null and g.longitude is not null
        order by 
          case 
            when lower(g.name)    like lower(concat(:q, '%')) then 0
            when lower(g.name)    like lower(concat('%', :q, '%')) then 1
            when lower(g.address) like lower(concat(:q, '%')) then 2
            else 3
          end,
          g.updatedAt desc
        """)
    Page<SearchRow> searchByNameOrAddress(@Param("q") String q, Pageable pageable);
}
