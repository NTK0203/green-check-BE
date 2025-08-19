package com.greencheck.controller;

import com.greencheck.dto.ImportResult;
import com.greencheck.service.ExcelImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/green-buildings")
@RequiredArgsConstructor
public class AdminImportController {

    private final ExcelImportService excelImportService;

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importExcel(@RequestPart("file") MultipartFile file,
                                    @RequestParam(defaultValue = "true") boolean geocode) {
        return excelImportService.importExcel(file, geocode);
    }
}
