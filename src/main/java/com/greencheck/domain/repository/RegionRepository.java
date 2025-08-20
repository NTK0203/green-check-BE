package com.greencheck.domain.repository;

import com.greencheck.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, String> {
    Optional<Region> findByName(String name);
}
