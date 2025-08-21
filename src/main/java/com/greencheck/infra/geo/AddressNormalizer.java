package com.greencheck.infra.geo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AddressNormalizer {

    private AddressNormalizer() {}

    // 사용 x
    public static List<String> buildCandidates(String region, String sigungu, String location) {
        Set<String> ordered = new LinkedHashSet<>();

        // 1) location이 이미 풀주소 같으면 그 자체/정제본
        if (notBlank(location) && looksFullAddress(location)) {
            String a1 = normalizeSpaces(location.trim());
            ordered.add(a1);
            ordered.add(cleanForGeo(a1));
        }

        // 2) region + sigungu + location (일반 케이스)
        String composite = joinParts(region, sigungu, location);
        if (notBlank(composite)) {
            String a2 = normalizeSpaces(composite);
            ordered.add(a2);
            ordered.add(cleanForGeo(a2));
        }

        // 3) region + sigungu (상세 누락 시 행정명까지만)
        String adminOnly = joinParts(region, sigungu, null);
        if (notBlank(adminOnly)) {
            String a3 = normalizeSpaces(adminOnly);
            ordered.add(a3);
            ordered.add(cleanForGeo(a3));
        }

        // 4) region만
        if (notBlank(region)) {
            String a4 = normalizeSpaces(region);
            ordered.add(a4);
            ordered.add(cleanForGeo(a4));
        }

        // 빈/너무 짧으면 제거
        List<String> result = new ArrayList<>();
        for (String s : ordered) {
            if (notBlank(s) && s.length() >= 4) result.add(s);
        }
        return result;
    }

    private static boolean looksFullAddress(String s) {
        return s.contains("특별시") || s.contains("광역시") || s.contains("도 ")
                || s.contains("시 ") || s.contains("구 ") || s.contains("동 ");
    }

    private static String joinParts(String region, String sigungu, String location) {
        StringBuilder sb = new StringBuilder();
        if (notBlank(region)) sb.append(region.trim()).append(' ');
        if (notBlank(sigungu)) sb.append(sigungu.trim()).append(' ');
        if (notBlank(location)) sb.append(location.trim());
        return sb.toString().trim();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String normalizeSpaces(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    //주소 최적화 필요없는 부분 제거하고 주소로 검색
    public static String cleanForGeo(String address) {
        if (!notBlank(address)) return address;
        String a = address.trim();

        // 흔한 꼬리표 제거: "외3필지", "일대", "일원", "일부" 등
        a = a.replaceAll("\\s*외\\d*필지.*$", "");
        a = a.replaceAll("\\s*(일대|일원|일부)\\s*$", "");

        // 콤마 이후 잘라내기: "43-6,7번지" → "43-6"
        a = a.replaceAll(",.*?(번지)?$", "");

        // "지하" 표기/층 정보 등 제거
        a = a.replaceAll("\\s*지하\\s*\\d*\\s*", " ");
        a = a.replaceAll("\\s*B\\d+\\s*", " ");

        // "번지" 접미사 제거 (카카오는 지번에 '번지' 없어도 잘 찾음)
        a = a.replaceAll("\\s*번지\\s*$", "");

        // 연속 공백 정리
        a = a.replaceAll("\\s+", " ").trim();

        // 너무 짧아지면 원본 유지
        if (a.length() < 4) return address.trim();
        return a;
    }
}
