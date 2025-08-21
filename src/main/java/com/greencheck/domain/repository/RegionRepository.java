package com.greencheck.domain.repository;

import com.greencheck.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, String> {
    Optional<Region> findByRegionCode(String regionCode);
    Optional<Region> findByName(String name);
    Optional<Region> findFirstByName(String name);     // 이름으로 우선 매칭 시 사용 가능
    Optional<Region> findByNameAndParentCode(String name, String parentCode); // 예: ("종로구", "11")
    List<Region> findByRegionCodeStartingWith(String prefix);
}
