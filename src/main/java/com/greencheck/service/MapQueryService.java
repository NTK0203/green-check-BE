package com.greencheck.service;

import com.greencheck.domain.repository.GreenBuildingMapRepository;
import com.greencheck.dto.MarkerDto;
import com.greencheck.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
