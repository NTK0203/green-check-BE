package com.greencheck.controller;

import com.greencheck.dto.EmissionKrResponse;
import com.greencheck.service.EmissionMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/map/emissions")
@RequiredArgsConstructor
public class MapEmissionController {

    private final EmissionMapService service;

    /**
     * 예) GET /map/emissions/kr?year=2022&limit=17
     */
    @GetMapping("/kr")
    public EmissionKrResponse getKr(
            @RequestParam(defaultValue = "2022") String year,
            @RequestParam(required = false) Integer limit
    ) {
        return service.getKrEmissions(year, limit);
    }
}
