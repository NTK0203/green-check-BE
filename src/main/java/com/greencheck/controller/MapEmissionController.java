package com.greencheck.controller;

import com.greencheck.dto.EmissionKrResponse;
import com.greencheck.service.EmissionMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.greencheck.dto.EmissionKrSeoulResponse;
import com.greencheck.service.MapEmissionV2Service;

@RestController
@RequestMapping("/map/emissions")
@RequiredArgsConstructor
public class MapEmissionController {

    private final EmissionMapService service;
    private final MapEmissionV2Service v2Service;

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

    //서울
    @GetMapping("/seoul")
    public ResponseEntity<EmissionKrSeoulResponse> seoulV2(
            @RequestParam String year,
            @RequestParam String monthRange,
            @RequestParam(defaultValue = "sum") String state,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(v2Service.getSeoul(year, monthRange, state, limit));
    }
}
