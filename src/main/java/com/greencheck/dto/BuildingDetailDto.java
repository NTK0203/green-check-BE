package com.greencheck.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BuildingDetailDto {
    private Long id;
    private String name;
    private String address;
    private Double lat;          // BigDecimal -> Double 변환해서 노출
    private Double lng;          // BigDecimal -> Double 변환해서 노출
    private String gradeCode;
    private String useCategory;
    private Integer certYear;
    private BigDecimal floorArea;
    private boolean isPublic;
    private String certAgency;
    private LocalDateTime updatedAt;
}
