package com.greencheck.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "green_building_map")
@Getter
@NoArgsConstructor
public class GreenBuildingMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "building_id")
    private Long id;

    @Column(length = 200)
    private String name;

    @Column(length = 300)
    private String address;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "grade_code", length = 10)
    private String gradeCode;

    @Column(name = "use_category", length = 60)
    private String useCategory;

    @Column(name = "cert_year")
    private Integer certYear;

    @Column(name = "floor_area", precision = 12, scale = 2)
    private BigDecimal floorArea;

    @Column(name = "is_public")
    private boolean isPublic;

    @Column(name = "cert_agency", length = 40)
    private String certAgency;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}