package com.greencheck.domain.repository;

import com.greencheck.domain.CarbonEmission;
import com.greencheck.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarbonEmissionRepository extends JpaRepository<CarbonEmission, Long> {
    Optional<CarbonEmission> findByRegionAndYearAndSource(Region region, String year, String source);

    List<CarbonEmission> findAllByRegion_RegionCodeStartingWithAndYearAndSourceIn(
            String prefix, String year, List<String> sources
    );

}
