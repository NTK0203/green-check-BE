package com.greencheck.infra.odcloud;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OdcloudEmissionsClient {

    private final RestTemplate restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofMillis(3000))
            .setReadTimeout(Duration.ofMillis(5000))
            .build();

    @Value("${odcloud.service.key}") private String serviceKey;
    @Value("${odcloud.dataset.path}") private String datasetPath;

    /**
     * @param year "2022" 등
     * @return 시도명 -> (CO2 + CH4 + N2O) 합계
     */
    @SuppressWarnings("unchecked")
    public Map<String, Double> fetchRoadSectorSumBySido(String year) {
        try {
            // 1) 키 준비: 이미 %가 있으면 인코딩키, 없으면 여기서 1회만 인코딩
            String raw = Optional.ofNullable(serviceKey).orElse("").trim();
            boolean alreadyEncoded = raw.contains("%");
            String keyForUrl = alreadyEncoded ? raw : UriUtils.encodeQueryParam(raw, StandardCharsets.UTF_8);

            // 2) URL 직접 조립(빌더 사용 X: 이중 인코딩 방지)
            String url = datasetPath
                    + "?page=1"
                    + "&perPage=5000"
                    + "&serviceKey=" + keyForUrl;

            if (log.isDebugEnabled()) {
                String masked = url.replaceAll("(serviceKey=)[^&]+", "$1***");
                log.debug("ODCLOUD URL = {}", masked);
                log.debug("ODCLOUD key alreadyEncoded? {}", alreadyEncoded);
            }

            ResponseEntity<Map> res = restTemplate.exchange(
                    URI.create(url),
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    Map.class
            );

            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
                log.warn("ODCLOUD non-2xx or null body: status={}", res.getStatusCode());
                return Map.of();
            }

            Object dataObj = res.getBody().get("data");
            if (!(dataObj instanceof List<?> list)) {
                log.warn("ODCLOUD response missing 'data' array");
                return Map.of();
            }
            List<Map<String, Object>> rows = (List<Map<String, Object>>) list;
            log.debug("ODCLOUD rows size = {}", rows.size());

            // 이 데이터셋은 이미 '도로부문' 데이터. 부문 필터 제거.
            List<Map<String, Object>> filtered = rows.stream()
                    .filter(r -> yearEquals(r, year))
                    .collect(Collectors.toList());

            log.debug("ODCLOUD filtered size (year={}) = {}", year, filtered.size());

            Map<String, Double> bySido = new LinkedHashMap<>();
            for (Map<String, Object> r : filtered) {
                String sidoRaw = extractSidoName(r); // 예: "서 울"
                double sum = extractGas(r, // 이산화탄소
                        "이산화탄소 (CO2)", "이산화탄소", "CO2")
                        + extractGas(r,             // 메탄
                        "메탄 (CH4)", "메탄", "CH4")
                        + extractGas(r,             // 아산화질소
                        "아산화질소(N2O)", "아산화질소 (N2O)", "아산화질소", "N2O");

                bySido.merge(sidoRaw, sum, Double::sum);
            }
            return bySido;

        } catch (Exception e) {
            log.error("ODCLOUD fetch 실패", e);
            return Map.of();
        }
    }


    private static boolean yearEquals(Map<String, Object> row, String year) {
        // 실제 응답은 "년도": 2022 (숫자) 형태
        for (String k : List.of("년도", "연도", "시점", "year", "YEAR")) {
            Object v = row.get(k);
            if (v == null) continue;
            String vs = v.toString().trim();
            if (vs.equals(year)) return true;
            // 숫자로 들어오면 "2022" 비교가 안될 수 있어 보조 비교
            try {
                if (String.valueOf(Integer.parseInt(vs)).equals(year)) return true;
            } catch (NumberFormatException ignored) {}
        }
        return false;
    }

    private static String extractSidoName(Map<String, Object> row) {
        // 이 데이터셋은 "구 분" 키를 사용(예: "서 울")
        for (String k : List.of("구 분", "구분", "시도", "지역", "지역명", "광역시도")) {
            Object v = row.get(k);
            if (v != null && !v.toString().isBlank()) return v.toString().trim();
        }
        return "미상";
    }

    private static double extractGas(Map<String, Object> row, String... keys) {
        for (String k : keys) {
            Object v = row.get(k);
            if (v != null && !v.toString().isBlank()) {
                try {
                    return Double.parseDouble(v.toString().replace(",", ""));
                } catch (NumberFormatException ignored) {}
            }
        }
        return 0.0;
    }
}
