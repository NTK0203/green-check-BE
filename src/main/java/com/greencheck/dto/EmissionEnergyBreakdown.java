package com.greencheck.dto;

import java.math.BigDecimal;

public record EmissionEnergyBreakdown(
        BigDecimal elec,
        BigDecimal gas,
        BigDecimal heat
) {
    public static EmissionEnergyBreakdown of(BigDecimal elec, BigDecimal gas) {
        return new EmissionEnergyBreakdown(n(elec), n(gas), BigDecimal.ZERO);
    }
    private static BigDecimal n(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
