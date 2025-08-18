package com.greencheck.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "region")
@Getter
@NoArgsConstructor
public class Region {

    @Id
    @Column(name = "region_code", length = 20)
    private String regionCode;

    @Column(name = "parent_code", length = 20)
    private String parentCode;

    @Column(length = 20)
    private String name;

    @Column(length = 10)
    private String level;

    @OneToMany(mappedBy = "region", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CarbonEmission> carbonEmissions = new ArrayList<>();
}