package com.greencheck.dto;

import java.math.BigDecimal;

public record MarkerDto(
        Long id,
        BigDecimal lat,
        BigDecimal lng,
        Double distance,

        String name,
        String address,
        String useCategory,
        Integer certYear,
        String gradeCode)
{ }
