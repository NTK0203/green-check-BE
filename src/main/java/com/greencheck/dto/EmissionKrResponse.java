package com.greencheck.dto;

import java.util.List;

public record EmissionKrResponse(
        String year,
        String unit,
        int count,
        List<EmissionPointDto> points
) {}
