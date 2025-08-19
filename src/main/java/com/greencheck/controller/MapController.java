package com.greencheck.controller;

import com.greencheck.dto.MarkerDto;
import com.greencheck.dto.PagedResponse;
import com.greencheck.service.MapQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/map")
@RequiredArgsConstructor
@Validated
public class MapController {

    private final MapQueryService svc;

    @GetMapping("/green-buildings")
    public PagedResponse<MarkerDto> search(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "3000") Integer radiusMeters,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String gradeCode,
            @RequestParam(required = false) String useCategory,
            @RequestParam(required = false) Integer certYear,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "500") Integer size
    ) {
        return svc.search(lat, lng, radiusMeters, q, gradeCode, useCategory, certYear, page, size);
    }

    // 필터 목록 제공
    @GetMapping("/green-buildings/filters")
    public Map<String, Object> filters() {
        return svc.getFilters();
    }
}
