package com.greencheck.dto;

import java.math.BigDecimal;

public record EmissionPointV2(
        String regionCode,
        String regionName,
        BigDecimal lat,
        BigDecimal lng,
        BigDecimal value, // elec + gas
        EmissionEnergyBreakdown byEnergy
) {}
