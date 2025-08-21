package com.greencheck.controller;

import com.greencheck.dto.EmissionKrResponse;
import com.greencheck.service.EmissionMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/map/emissions")
@RequiredArgsConstructor
public class MapEmissionController {

    private final EmissionMapService service;

    //전국
    @GetMapping("/kr")
    public EmissionKrResponse getKr(
            @RequestParam(defaultValue = "2022") String year,
            @RequestParam(required = false) Integer limit
    ) {
        return service.getKrEmissions(year, limit);
    }

    //경기도
    @GetMapping(value = "/gg", produces = MediaType.APPLICATION_JSON_VALUE)
    public EmissionKrResponse getGg(
            @RequestParam(defaultValue = "2023") String year,
            @RequestParam(required = false) Integer limit
    ) {
        return service.getGgEmissions(year, limit);
    }
}
