package com.greencheck.service;

import com.greencheck.domain.Region;
import com.greencheck.domain.CarbonEmission;
import com.greencheck.domain.repository.CarbonEmissionRepository;
import com.greencheck.domain.repository.RegionRepository;
import com.greencheck.dto.EmissionEnergyBreakdown;
import com.greencheck.dto.EmissionKrSeoulResponse;
import com.greencheck.dto.EmissionPointV2;
import com.greencheck.infra.geo.KakaoGeocodingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MapEmissionV2Service {

    private final RegionRepository regionRepo;
    private final CarbonEmissionRepository ceRepo;
    private final KakaoGeocodingClient kakaoGeo;

    @Transactional
    public EmissionKrSeoulResponse getSeoul(String year, String monthRangeRaw, String state, Integer limit) {

        String monthRange = monthRangeRaw.replace("~", "-").trim();
        String srcElec = "ELEC_M" + monthRange.replace("-", "_");
        String srcGas  = "GAS_M"  + monthRange.replace("-", "_");

        // 서울(11...)가져오고 서울특별시는 빼고
        List<Region> regions = new ArrayList<>(regionRepo.findByRegionCodeStartingWith("11"));
        regions.removeIf(r -> r.getRegionCode() == null
                || r.getRegionCode().length() <= 2
                || "11".equals(r.getRegionCode()));

        // 배출량 조회
        List<CarbonEmission> emissions = ceRepo.findAllByRegion_RegionCodeStartingWithAndYearAndSourceIn(
                "11", year, List.of(srcElec, srcGas)
        );

        // 전기 / 가스 합계 맵
        Map<String, BigDecimal> elecMap = new HashMap<>();
        Map<String, BigDecimal> gasMap  = new HashMap<>();

        emissions.forEach(ce -> {
            String code = ce.getRegion().getRegionCode();
            BigDecimal v = ce.getTotalTCO2();
            if (ce.getSource().startsWith("ELEC")) elecMap.merge(code, v, BigDecimal::add);
            if (ce.getSource().startsWith("GAS"))  gasMap.merge(code,  v, BigDecimal::add);
        });

        // 포인트 생성
        List<EmissionPointV2> points = new ArrayList<>();

        for (Region r : regions) {
            BigDecimal elec  = elecMap.getOrDefault(r.getRegionCode(), BigDecimal.ZERO);
            BigDecimal gas   = gasMap.getOrDefault(r.getRegionCode(), BigDecimal.ZERO);
            BigDecimal total = elec.add(gas); // heat는 합산하지 않음

            // 좌표 없으면 지오코딩하여 Region에 저장
            BigDecimal lat = r.getLatitude();
            BigDecimal lng = r.getLongitude();
            if (lat == null || lng == null) {
                BigDecimal[] geo = ensureLatLng(r);
                lat = geo[0];
                lng = geo[1];
            }

            points.add(new EmissionPointV2(
                    r.getRegionCode(),
                    r.getName(),
                    lat,
                    lng,
                    total,
                    EmissionEnergyBreakdown.of(elec, gas)
            ));
        }

        // 값 내림차순 정렬 + limit
        points.sort((a, b) -> b.value().compareTo(a.value()));
        if (limit != null && limit > 0 && limit < points.size()) {
            points = points.subList(0, limit);
        }

        return new EmissionKrSeoulResponse(
                year, monthRange,
                (state == null || state.isBlank()) ? "sum" : state.toLowerCase(),
                "tCO2e",
                points.size(),
                points
        );
    }

    //카카오 지오코딩
    private BigDecimal[] ensureLatLng(Region r) {
        BigDecimal lat = r.getLatitude();
        BigDecimal lng = r.getLongitude();
        if (lat != null && lng != null) return new BigDecimal[]{lat, lng};

        String query = buildSeoulQuery(r.getName());

        return kakaoGeo.geocode(query)
                .map(p -> {
                    BigDecimal latBD = BigDecimal.valueOf(p.lat());
                    BigDecimal lngBD = BigDecimal.valueOf(p.lng());
                    setField(r, "latitude",  latBD);
                    setField(r, "longitude", lngBD);
                    regionRepo.save(r);
                    log.debug("[GEO] {} -> {},{}", query, latBD, lngBD);
                    return new BigDecimal[]{latBD, lngBD};
                })
                .orElseGet(() -> new BigDecimal[]{
                        new BigDecimal("37.5665"),
                        new BigDecimal("126.9780")
                });
    }

    private String buildSeoulQuery(String regionName) {
        if (regionName == null || regionName.isBlank()) return "서울특별시청";
        if (regionName.endsWith("구")) return "서울특별시 " + regionName;
        if (regionName.endsWith("구청")) return "서울특별시 " + regionName.substring(0, regionName.length() - 1);
        return "서울특별시 " + regionName;
    }

    private static void setField(Object target, String field, Object v) {
        try {
            var f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, v);
        } catch (Exception e) {
            throw new RuntimeException("Region 좌표 저장 실패: " + e.getMessage(), e);
        }
    }
}
