package com.greencheck.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "score_band")
@Getter
@NoArgsConstructor
public class ScoreBand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "band_id")
    private Long id;

    @Column(name = "min_score", nullable = false)
    private int minScore;

    @Column(name = "max_score", nullable = false)
    private int maxScore;

    @Column(name = "mid_score", nullable = false)
    private int midScore;

    @Column(nullable = false, length = 50)
    private String label;

    @OneToMany(mappedBy = "scoreBand", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScoreBandSuggestion> suggestions = new ArrayList<>();
}