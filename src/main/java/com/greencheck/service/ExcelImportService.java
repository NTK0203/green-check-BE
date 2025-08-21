package com.greencheck.service;

import com.greencheck.domain.GreenBuildingMap;
import com.greencheck.domain.repository.GreenBuildingMapRepository;
import com.greencheck.dto.ImportResult;
import com.greencheck.infra.geo.KakaoGeocodingClient;
import com.greencheck.infra.geo.AddressNormalizer; // [KEEP] 정제만 사용
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Iterator;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final GreenBuildingMapRepository repo;
    private final KakaoGeocodingClient kakao;

    @Value("${app.geocoding.per-request-limit:50}")
    private int perRequestLimit;

    @Value("${app.geocoding.sleep-ms-between-calls:120}")
    private long sleepBetweenCallsMs;

    @Transactional
    public ImportResult importExcel(MultipartFile file, boolean doGeocode) {
        int rows = 0, geocoded = 0, inserted = 0, updated = 0;
        int attempted = 0;

        log.info("[IMPORT] START doGeocode={}, perRequestLimit={}, sleepMs={}",
                doGeocode, perRequestLimit, sleepBetweenCallsMs);

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);

            // 헤더 2줄 스킵 (엑셀 파일 구조 기준)
            Iterator<Row> it = sheet.iterator();
            if (it.hasNext()) it.next();
            if (it.hasNext()) it.next();

            // 엑셀에 해당하는 열 매핑(인덱스 0부터 시작)
            final int IDX_NAME       = 15;
            final int IDX_REGION     = 16;
            final int IDX_SIGUNGU    = 17;
            final int IDX_LOCATION   = 18;
            final int IDX_GRADE      = 13;
            final int IDX_USE        = 11;
            final int IDX_YEAR       = 5;
            final int IDX_AREA       = 25;
            final int IDX_SECTOR     = 19;
            final int IDX_AGENCY     = 3;

            int callCount = 0;

            while (it.hasNext()) {
                Row r = it.next();
                rows++;

                String name        = cellString(r, IDX_NAME);
                String region      = cellString(r, IDX_REGION);
                String sigungu     = cellString(r, IDX_SIGUNGU);
                String location    = cellString(r, IDX_LOCATION);
                String gradeRaw    = cellString(r, IDX_GRADE);
                String useCategory = cellString(r, IDX_USE);
                Integer certYear   = toInt(cellString(r, IDX_YEAR));
                BigDecimal floorArea = toDecimal(cellString(r, IDX_AREA));
                String sector      = cellString(r, IDX_SECTOR);
                String certAgency  = cellString(r, IDX_AGENCY);

                // location만 사용 (정제는 cleanForGeo로)
                String rawAddress = (location == null ? null : location.trim());
                String addressForSave = (rawAddress == null || rawAddress.isBlank())
                        ? null
                        : AddressNormalizer.cleanForGeo(rawAddress);

                if (rows <= 5) {
                    log.info("[ADDR] row {} name='{}' region='{}' sigungu='{}' location(raw)='{}' => address(save)='{}'",
                            rows, name, region, sigungu, location, addressForSave);
                }

                // upsert 기준: (name + addressForSave)
                GreenBuildingMap gb = repo.findFirstByNameAndAddress(name, addressForSave)
                        .orElseGet(GreenBuildingMap::new);

                boolean isNew = (gb.getId() == null);
                gb.setName(name);
                gb.setAddress(addressForSave);
                gb.setGradeCode(gradeToCode(normalizeGrade(gradeRaw)));
                gb.setUseCategory(useCategory);
                gb.setCertYear(certYear);
                gb.setFloorArea(floorArea);
                gb.setPublic("공공".equals(sector));
                gb.setCertAgency(certAgency);
                gb.setUpdatedAt(LocalDateTime.now());

                // location 하나만 지오코딩
                if (doGeocode && (gb.getLatitude() == null || gb.getLongitude() == null)) {
                    if (addressForSave != null && !addressForSave.isBlank()) {
                        if (callCount < perRequestLimit) {
                            attempted++;
                            log.debug("[GEO-TRY {}/{}] {}", callCount + 1, perRequestLimit, addressForSave);

                            kakao.geocode(addressForSave).ifPresent(p -> {
                                gb.setLatitude(BigDecimal.valueOf(p.lat()));  // y(lat)
                                gb.setLongitude(BigDecimal.valueOf(p.lng())); // x(lng)
                            });

                            callCount++;

                            if (gb.getLatitude() != null && gb.getLongitude() != null) {
                                geocoded++;
                                log.info("[GEO-OK ] {} -> {}, {}", addressForSave, gb.getLatitude(), gb.getLongitude());
                            } else {
                                log.warn("[GEO-NO ] {}", addressForSave);
                            }

                            if (sleepBetweenCallsMs > 0) {
                                try { Thread.sleep(sleepBetweenCallsMs); } catch (InterruptedException ignored) {}
                            }
                        } else {
                            log.warn("[GEO-SKIP] perRequestLimit({}) 초과로 지오코딩 중단", perRequestLimit);
                        }
                    } else if (rows <= 5) {
                        log.debug("[GEO-SKIP] address empty for row {}", rows);
                    }
                } else if (rows <= 5) {
                    log.debug("[GEO-SKIP] doGeocode={}, hasLatLng? {}", doGeocode,
                            (gb.getLatitude() != null && gb.getLongitude() != null));
                }

                repo.save(gb);
                if (isNew) inserted++; else updated++;
            }
        } catch (Exception e) {
            log.error("[IMPORT] FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Excel import failed: " + e.getMessage(), e);
        }

        log.info("[IMPORT] END rows={}, attemptedGeo={}, geocoded={}, inserted={}, updated={}",
                rows, attempted, geocoded, inserted, updated);

        return new ImportResult(rows, geocoded, inserted, updated);
    }

    // ====== 유틸 ======
    private static String cellString(Row row, int idx) {
        Cell cell = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toString();
            String s = String.valueOf(cell.getNumericCellValue());
            if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
            return s;
        }
        if (cell.getCellType() == CellType.BOOLEAN) return Boolean.toString(cell.getBooleanCellValue());
        return null;
    }

    private static Integer toInt(String s) {
        try { return (s == null || s.isBlank()) ? null : (int) Double.parseDouble(s); }
        catch (Exception e) { return null; }
    }

    private static BigDecimal toDecimal(String s) {
        try { return (s == null || s.isBlank()) ? null : new BigDecimal(s.replace(",", "")); }
        catch (Exception e) { return null; }
    }

    private static String normalizeGrade(String gradeRaw) {
        if (gradeRaw == null) return null;
        int i = gradeRaw.indexOf('(');
        String base = (i > 0) ? gradeRaw.substring(0, i) : gradeRaw;
        return base.replace("등급", "").trim();
    }

    private static String gradeToCode(String gradeKr) {
        if (gradeKr == null) return null;
        if (gradeKr.contains("최우수")) return "EXCELLENT";
        if (gradeKr.contains("우수"))   return "VERY_GOOD";
        if (gradeKr.contains("우량"))   return "GOOD";
        if (gradeKr.contains("일반"))   return "BASIC";
        return gradeKr;
    }
}
