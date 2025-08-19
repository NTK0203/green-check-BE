package com.greencheck.service;

import com.greencheck.domain.repository.GreenBuildingMapRepository;
import com.greencheck.dto.MarkerDto;
import com.greencheck.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.*;

import com.greencheck.dto.ItemsResponse;
import com.greencheck.dto.SearchItemDto;
import java.math.BigDecimal;

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

    //텍스트 검색
    public ItemsResponse<SearchItemDto> searchByText(String q, Integer limit) {
        String keyword = (q == null) ? "" : q.trim();
        int max = (limit == null) ? 10 : Math.max(1, Math.min(limit, 20)); // 1~20, 기본 10

        var page = repo.searchByNameOrAddress(keyword, PageRequest.of(0, max));

        List<SearchItemDto> items = page.getContent().stream()
                .map(r -> new SearchItemDto(
                        r.getId(),
                        buildLabel(r.getName(), r.getAddress()),
                        nvl(r.getName()),
                        nvl(r.getAddress()),
                        toDouble(r.getLat()),
                        toDouble(r.getLng())
                ))
                .toList();

        return new ItemsResponse<>(items);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
    private static Double toDouble(BigDecimal bd) { return bd == null ? null : bd.doubleValue(); }

    private static String buildLabel(String name, String address) {
        StringBuilder sb = new StringBuilder();
        if (name != null && !name.isBlank()) sb.append(name.trim());
        if (address != null && !address.isBlank()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(address.trim());
        }
        // 둘 다 비어있다면 id만이라도 넣고 싶다면 여기서 처리할 수 있음
        return sb.toString();
    }
}
