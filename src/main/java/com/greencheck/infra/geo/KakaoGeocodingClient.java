// KakaoGeocodingClient.java
package com.greencheck.infra.geo;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;   // ✅ 추가 import

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class KakaoGeocodingClient {

    private final RestTemplate restTemplate;
    private final String apiKey;

    public KakaoGeocodingClient(
            RestTemplateBuilder builder,
            @Value("${app.http.connect-timeout-ms:2000}") int cto,
            @Value("${app.http.read-timeout-ms:3000}") int rto,
            @Value("${kakao.api.key:}") String apiKey
    ) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(cto))
                .setReadTimeout(Duration.ofMillis(rto))
                .build();
        this.apiKey = apiKey;
    }

    @PostConstruct
    void logKeyPresence() {
        log.info("Kakao API key present? {}", (apiKey != null && !apiKey.isBlank()));
    }

    public Optional<Point> geocode(String address) {
        try {
            if (address == null || address.isBlank()) return Optional.empty();

            // UriComponentsBuilder 로 쿼리 파라미터 추가(정확히 1번만 인코딩)
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://dapi.kakao.com/v2/local/search/address.json")
                    .queryParam("query", address.trim())
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> res = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class
            );

            // 디버그 로그 (상태/바디 확인)
            log.debug("[GEO] request addr={}, status={}, body={}", address, res.getStatusCode(), res.getBody());

            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) return Optional.empty();

            var documents = (java.util.List<Map<String, Object>>) res.getBody().get("documents");
            if (documents == null || documents.isEmpty()) return Optional.empty();

            var first = documents.get(0);
            double lng = Double.parseDouble(first.get("x").toString()); // Kakao: x=lng
            double lat = Double.parseDouble(first.get("y").toString()); // Kakao: y=lat
            return Optional.of(new Point(lat, lng));

        } catch (Exception e) {
            log.error("Geocoding failed for address={}", address, e);
            return Optional.empty();
        }
    }

    public record Point(double lat, double lng) {}
}
