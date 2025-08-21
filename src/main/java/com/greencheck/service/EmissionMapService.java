package com.greencheck.service;

import com.greencheck.dto.EmissionKrResponse;
import com.greencheck.dto.EmissionPointDto;
import com.greencheck.infra.geo.KakaoGeocodingClient;
import com.greencheck.infra.odcloud.OdcloudEmissionsClient;
import com.greencheck.infra.odcloud.GgEmissionsClient;
import com.greencheck.domain.Region;
import com.greencheck.domain.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmissionMapService {

    private final OdcloudEmissionsClient odcloud;
    private final GgEmissionsClient ggClient;
    private final KakaoGeocodingClient kakao;
    private final RegionRepository regionRepo;

    @Transactional
    public EmissionKrResponse getKrEmissions(String year, Integer limit) {
        Map<String, Double> bySidoRaw = odcloud.fetchRoadSectorSumBySido(year);

        List<EmissionPointDto> points = new ArrayList<>();
        for (Map.Entry<String, Double> e : bySidoRaw.entrySet()) {
            String rawName = e.getKey();
            String normalized = normalizeSidoName(rawName);
            double value = e.getValue();

            Optional<Region> opt = regionRepo.findByName(normalized);
            if (opt.isEmpty()) {
                log.warn("Region not found for name(raw='{}', normalized='{}')", rawName, normalized);
                continue;
            }
            Region region = opt.get();

            if (region.getLatitude() == null || region.getLongitude() == null) {
                kakao.geocode(region.getName()).ifPresent(p ->
                        region.setCenter(BigDecimal.valueOf(p.lat()), BigDecimal.valueOf(p.lng())));
            }
            if (region.getLatitude() == null || region.getLongitude() == null) {
                log.warn("No coordinates for region='{}', skip", region.getName());
                continue;
            }

            points.add(new EmissionPointDto(
                    region.getRegionCode(),
                    region.getName(),
                    region.getLatitude(),
                    region.getLongitude(),
                    value
            ));
        }

        points.sort((a, b) -> Double.compare(b.value(), a.value()));
        if (limit != null && limit > 0 && limit < points.size()) {
            points = points.subList(0, limit);
        }

        return new EmissionKrResponse(
                year,
                "tCO2e",
                points.size(),
                points
        );
    }

    // 경기도(시/군)
    @Transactional
    public EmissionKrResponse getGgEmissions(String year, Integer limit) {
        var rows = ggClient.fetch(year, limit);  // 경기도 오픈API 호출

        List<EmissionPointDto> points = new ArrayList<>();
        for (var r : rows) {
            String code = r.regionCode();
            String name = normalizeGgName(r.regionName());   // 특례시 등 표준화
            Double value = r.value();
            if (name == null || value == null) continue;

            // 1) regionCode 우선, 없으면 이름, 그래도 없으면 firstByName
            Region region = null;
            if (code != null) {
                region = regionRepo.findByRegionCode(code).orElse(null);
            }
            if (region == null) {
                region = regionRepo.findByName(name).orElse(null);
            }
            if (region == null) {
                region = regionRepo.findFirstByName(name).orElse(null);
            }
            if (region == null) {
                log.warn("GG Region not found (code={}, name={})", code, name);
                continue;
            }

            // 2) 좌표 없으면 카카오 지오코딩
            if (region.getLatitude() == null || region.getLongitude() == null) {
                var geoOpt = kakao.geocode("경기도 " + region.getName());
                if (geoOpt.isPresent()) {
                    var p = geoOpt.get();
                    region.setCenter(BigDecimal.valueOf(p.lat()), BigDecimal.valueOf(p.lng()));
                }
            }
            if (region.getLatitude() == null || region.getLongitude() == null) {
                log.warn("No coordinates for GG region='{}', skip", region.getName());
                continue;
            }

            points.add(new EmissionPointDto(
                    region.getRegionCode() != null ? region.getRegionCode() : code,
                    region.getName(),
                    region.getLatitude(),
                    region.getLongitude(),
                    value
            ));
        }

        // value 내림차순 + limit
        points.sort(Comparator.comparingDouble(EmissionPointDto::value).reversed());
        if (limit != null && limit > 0 && limit < points.size()) {
            points = points.subList(0, limit);
        }

        return new EmissionKrResponse(year, "tCO2e", points.size(), points);
    }


    //서울->서울특별시 로 표준화
    private String normalizeSidoName(String name) {
        if (name == null) return "";
        String n = name.replaceAll("\\s+", ""); //공백제거

        Map<String, String> map = Map.ofEntries(
                Map.entry("서울", "서울특별시"),
                Map.entry("부산", "부산광역시"),
                Map.entry("대구", "대구광역시"),
                Map.entry("인천", "인천광역시"),
                Map.entry("광주", "광주광역시"),
                Map.entry("대전", "대전광역시"),
                Map.entry("울산", "울산광역시"),
                Map.entry("세종", "세종특별자치시"),
                Map.entry("경기", "경기도"),
                Map.entry("강원", "강원특별자치도"),
                Map.entry("충북", "충청북도"),
                Map.entry("충남", "충청남도"),
                Map.entry("전북", "전라북도"),
                Map.entry("전남", "전라남도"),
                Map.entry("경북", "경상북도"),
                Map.entry("경남", "경상남도"),
                Map.entry("제주", "제주특별자치도")
        );
        return map.getOrDefault(n, n);
    }
    // 경기도 특례시 표기 등 표준화(예: 고양특례시 → 고양시)
    private String normalizeGgName(String name) {
        if (name == null) return null;
        String n = name.trim().replaceAll("\\s+","");
        Map<String,String> map = Map.of(
                "수원특례시","수원시",
                "용인특례시","용인시",
                "고양특례시","고양시",
                "성남특례시","성남시"
        );
        return map.getOrDefault(n, n);
    }
}