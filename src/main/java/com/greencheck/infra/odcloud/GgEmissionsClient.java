package com.greencheck.infra.odcloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Slf4j
@Component
public class GgEmissionsClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    private final ObjectMapper om = new ObjectMapper();

    public GgEmissionsClient(
            RestTemplateBuilder builder,
            @Value("${gg.api.timeout-ms:3000}") int timeoutMs,
            @Value("${gg.api.url}") String baseUrl,
            @Value("${gg.api.key}") String apiKey
    ) {
        DefaultUriBuilderFactory f = new DefaultUriBuilderFactory();
        f.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        this.restTemplate = builder
                .uriTemplateHandler(f)
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;

        if (!this.baseUrl.endsWith("GGSIGUNGREENGASEMSTM")) {
            log.error("[GG] baseUrl 서비스명이 잘못됨. 기대=GGSIGUNGREENGASEMSTM, 현재={}", this.baseUrl);
        }
    }


    public List<Row> fetch(String year, Integer limit) {
        int want = Optional.ofNullable(limit).orElse(100);
        final int PAGE_MAX = Math.min(want, 31);

        List<Row> all = new ArrayList<>();
        int page = 1;

        // 요청 연도
        String targetYear = null;

        while (all.size() < want) {
            String url = baseUrl
                    + "?KEY=" + apiKey
                    + "&Type=json"
                    + "&pIndex=" + page
                    + "&pSize=" + PAGE_MAX;

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.ACCEPT, "*/*");
                headers.set(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name());
                headers.set(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Safari/537.36");
                headers.set(HttpHeaders.REFERER, "https://data.gg.go.kr/");
                headers.set(HttpHeaders.CONNECTION, "close");

                HttpEntity<Void> entity = new HttpEntity<>(headers);
                log.debug("[GG] request url={}", url);

                ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
                    log.warn("[GG] non-2xx url={}, status={}, body={}", url, res.getStatusCode(), res.getBody());
                    break;
                }

                List<Map<String, Object>> rows = extractRows(res.getBody());
                if (rows.isEmpty()) {
                    log.warn("[GG] rows empty at page={}", page);
                    break;
                }

                // 처음 페이지에서 타깃 연도 결정
                if (page == 1) {
                    // 응답에 존재하는 연도들 수집
                    Set<String> years = new HashSet<>();
                    for (Map<String, Object> m : rows) {
                        Object yy = m.get("YY");
                        if (yy != null && !yy.toString().isBlank()) years.add(yy.toString());
                    }
                    if (year != null && years.contains(year)) {
                        targetYear = year;
                    } else {
                        // 존재하는 연도 중 가장 최신(숫자 최대) 선택
                        targetYear = years.stream()
                                .filter(s -> s.matches("\\d{4}"))
                                .max(Comparator.naturalOrder())
                                .orElse(null);
                        log.info("[GG] 요청연도={} → 사용연도(가장 최신)={}", year, targetYear);
                    }
                }

                int added = 0;
                for (Map<String, Object> m : rows) {
                    String yy = firstString(m, "YY");
                    if (targetYear != null && yy != null && !yy.equals(targetYear)) continue;

                    String code = firstString(m, "SIGUN_CD", "SIG_CD", "ADM_CD", "regionCode");
                    String name = firstString(m, "SIGUN_NM", "SIG_KOR_NM", "ADM_NM", "regionName");
                    Double val = firstDouble(m,
                            "GAS_EMISN_AMNT",   // 온실가스배출량(tonCO2-eq)
                            "STD_EMISN_AMNT",   // 기준배출량(tonCO2-eq)
                            "TOT_EMSS_AMT", "TOT_EMS_AMT",
                            "EM_VAL", "TOTAL", "value", "배출량");

                    if (name == null || val == null) continue;

                    all.add(new Row(code, name, val));
                    added++;
                    if (all.size() >= want) break;
                }

                log.info("[GG] page={}, fetched={}, total={}", page, added, all.size());
                if (added == 0) {
                    // 이 페이지에 타깃 연도 데이터가 없으면 다음 페이지 계속 본다.
                    // 더 이상 페이지가 없다고 판단되면 루프 종료.
                }

                page++;
            } catch (org.springframework.web.client.HttpStatusCodeException ex) {
                log.warn("[GG] try failed url={}, status={}, body={}", url, ex.getStatusCode(), ex.getResponseBodyAsString());
                break;
            } catch (Exception e) {
                log.warn("[GG] page {} failed. ex={}", page, e.toString());
                break;
            }
        }

        return all;
    }


    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRows(String raw) throws Exception {
        Map<String, Object> root = om.readValue(raw, Map.class);

        // case 1: { "data": [ {...}, ... ] }
        Object data = root.get("data");
        if (data instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map) {
            return (List<Map<String, Object>>) data;
        }

        // case 2: { "SERVICE_NAME": [ {"head":[...]}, {"row":[ {...}, ... ]} ] }
        for (Map.Entry<String, Object> e : root.entrySet()) {
            Object v = e.getValue();
            if (v instanceof List<?> lst) {
                for (Object item : lst) {
                    if (item instanceof Map<?, ?> m) {
                        Object row = ((Map<?, ?>) m).get("row");
                        if (row instanceof List<?> l2 && !l2.isEmpty() && l2.get(0) instanceof Map) {
                            return (List<Map<String, Object>>) row;
                        }
                    }
                }
            }
        }

        log.warn("[GG] unexpected payload, raw={}", raw);
        return List.of();
    }

    private static String firstString(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null && !v.toString().isBlank()) return v.toString();
        }
        return null;
    }

    private static Double firstDouble(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v == null) continue;
            try {
                return Double.valueOf(v.toString().replaceAll(",", ""));
            } catch (Exception ignored) {}
        }
        return null;
    }

    public record Row(String regionCode, String regionName, Double value) {}
}
