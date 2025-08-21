//package com.greencheck.controller;
//
//import com.greencheck.infra.geo.KakaoGeocodingClient;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/debug")
//@RequiredArgsConstructor
//public class DebugGeoController {
//    private final KakaoGeocodingClient kakao;
//
//    @GetMapping("/geocode")
//    public Map<String,Object> test(@RequestParam String addr) {
//        return kakao.geocode(addr)
//                .<Map<String,Object>>map(p -> Map.of("ok", true, "lat", p.lat(), "lng", p.lng()))
//                .orElseGet(() -> Map.of("ok", false));
//    }
//}
