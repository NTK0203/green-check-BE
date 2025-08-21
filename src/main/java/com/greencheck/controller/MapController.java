package com.greencheck.controller;

import com.greencheck.dto.*;
import com.greencheck.service.MapQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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

    //검색
    @GetMapping("/green-buildings/search")
    public ItemsResponse<SearchItemDto> searchByText(
            @RequestParam @NotBlank String q,
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) Integer limit
    ) {
        return svc.searchByText(q, limit);
    }

    // 필터 목록 제공
    @GetMapping("/green-buildings/filters")
    public Map<String, Object> filters() {
        return svc.getFilters();
    }

    // 상세조회
    @GetMapping("/green-buildings/{id}")
    public BuildingDetailDto detail(@PathVariable Long id) {
        return svc.getDetail(id);
    }
}