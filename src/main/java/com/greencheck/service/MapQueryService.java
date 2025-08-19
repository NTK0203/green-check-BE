package com.greencheck.service;

import com.greencheck.domain.repository.GreenBuildingMapRepository;
import com.greencheck.dto.MarkerDto;
import com.greencheck.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MapQueryService {
    private final GreenBuildingMapRepository repo;

    public PagedResponse<MarkerDto> search(
            double lat, double lng, Integer radiusMeters,
            String q, String gradeCode, String useCategory, Integer certYear,
            Integer page, Integer size
    ) {
        int r = (radiusMeters == null || radiusMeters <= 0) ? 3000 : radiusMeters;
        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? 500 : Math.min(size, 1000);

        var result = repo.searchMarkers(
                lat, lng, r,
                emptyToNull(q), emptyToNull(gradeCode), emptyToNull(useCategory), certYear,
                PageRequest.of(p - 1, s)
        );

        List<MarkerDto> items = result.getContent().stream()
                .map(row -> new MarkerDto(row.getId(), row.getLat(), row.getLng(), row.getDistance()))
                .toList();

        return new PagedResponse<>(p, s, result.getTotalElements(), items);
    }

    private static String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }

    //filter
    private static final Map<String, String> GRADE_LABELS = Map.of(
            "EXCELLENT",  "최우수",
            "VERY_GOOD",  "우수",
            "GOOD",       "우량",
            "BASIC",      "일반"
    );

    private static final Map<String, String> USE_LABELS = Map.of(
            "EXISTING_NON_RESIDENTIAL", "기존비주거용",
            "EXISTING_RESIDENTIAL",     "기존주거용",
            "MIXED_USE",                "복합건축물",
            "NEW_NON_RESIDENTIAL",      "신축비주거용",
            "NEW_RESIDENTIAL",          "신축주거용"
    );

    public Map<String, Object> getFilters() {
        // DB에서 distinct 값 조회
        List<String> gradeCodes = repo.findDistinctGradeCodes();
        List<String> useCategories = repo.findDistinctUseCategories();
        List<Integer> years = repo.findDistinctCertYears(); // desc 정렬되어 반환 가정

        // 코드/라벨 페어로 변환
        List<Map<String, String>> gradeList = gradeCodes.stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .map(code -> Map.of(
                        "code", code,
                        "label", GRADE_LABELS.getOrDefault(code, code)
                ))
                .toList();

        List<Map<String, String>> useList = useCategories.stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .map(code -> Map.of(
                        "code", code,
                        "label", USE_LABELS.getOrDefault(code, code)
                ))
                .toList();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("gradeCode", gradeList);
        res.put("useCategory", useList);
        res.put("years", years);

        return res;
    }

}
