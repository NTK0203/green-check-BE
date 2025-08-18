package com.greencheck.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "carbon_emission")
@Getter
@NoArgsConstructor
public class CarbonEmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "carbon_id")
    private Long id;

    @Column(length = 10, nullable = false)
    private String year;

    @Column(name = "total_tco2", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalTCO2;

    @Column(length = 20)
    private String source;

    @Column(name = "version_date")
    private LocalDate versionDate;

    @Column(length = 10)
    private String unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_code")
    private Region region;
}