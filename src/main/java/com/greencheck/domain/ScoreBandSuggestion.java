package com.greencheck.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "score_band_suggestion")
@Getter
@NoArgsConstructor
public class ScoreBandSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "suggestion_id")
    private Long id;

    @Column(nullable = false, length = 10)
    private String type;

    @Column(nullable = false, length = 300)
    private String text;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "band_id", nullable = false)
    private ScoreBand scoreBand;
}