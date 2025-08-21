package com.greencheck.dto;

import java.util.List;

public record EmissionKrSeoulResponse(
        String year,
        String monthRange,
        String state,
        String unit,
        int count,
        List<EmissionPointV2> points
) {}
