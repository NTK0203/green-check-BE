package com.greencheck.domain.repository;

import com.greencheck.domain.ScoreBand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ScoreBandRepository extends JpaRepository<ScoreBand, Long> {
    @Query("SELECT sb FROM ScoreBand sb WHERE sb.minScore <= :score AND sb.maxScore >= :score")
    Optional<ScoreBand> findBandByScore(@Param("score") int score);
}