package com.greencheck.dto;

import java.math.BigDecimal;

public record EmissionPointDto(
        String regionCode,
        String regionName,
        BigDecimal lat,
        BigDecimal lng,
        double value
) {}

