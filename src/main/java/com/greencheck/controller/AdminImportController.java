package com.greencheck.controller;

import com.greencheck.service.SeoulEmissionImportService;
import com.greencheck.dto.ImportResult;
import com.greencheck.service.ExcelImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/admin/green-buildings")
@RequiredArgsConstructor
public class AdminImportController {

    private final ExcelImportService excelImportService;
    private final SeoulEmissionImportService seoulImportService;

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importExcel(@RequestPart("file") MultipartFile file,
                                    @RequestParam(defaultValue = "true") boolean geocode) {
        return excelImportService.importExcel(file, geocode);
    }

    //서울시 온실가스배출량 import
    @PostMapping(value = "/emissions/seoul-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadSeoul(
            @RequestParam String year,
            @RequestParam String monthRange, // "1-5" or "1~5"
            @RequestPart(name = "elecFile", required = false) MultipartFile elecFile,
            @RequestPart(name = "gasFile",  required = false) MultipartFile gasFile
    ) {
        Map<String,Object> body = seoulImportService.importExcel(elecFile, gasFile, year, monthRange);
        return ResponseEntity.ok(body);
    }
}
