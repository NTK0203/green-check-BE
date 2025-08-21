package com.greencheck.infra.geo;

import lombok.Data;
import java.util.List;

@Data
public class KakaoGeoResponse {
    private List<Document> documents;

    @Data
    public static class Document {
        private String x; // 경도
        private String y; // 위도
        private String address_name;
    }
}
