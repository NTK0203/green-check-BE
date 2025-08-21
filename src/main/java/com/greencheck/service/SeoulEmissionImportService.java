package com.greencheck.service;

import com.greencheck.domain.CarbonEmission;
import com.greencheck.domain.Region;
import com.greencheck.domain.repository.CarbonEmissionRepository;
import com.greencheck.domain.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeoulEmissionImportService {

    private final RegionRepository regionRepo;
    private final CarbonEmissionRepository ceRepo;

    // 헤더 후보(자치구 컬럼 식별)
    private static final List<String> REGION_HEADER_CANDIDATES =
            List.of("자치구명", "자치구", "구명", "행정구", "행정구명");

    private static final DataFormatter DF = new DataFormatter();

    // 단위 맞춤 (값/1000)
    private static final BigDecimal DIV_1000 = new BigDecimal("1000");

    @Transactional
    public Map<String, Object> importExcel(MultipartFile elecFile,
                                           MultipartFile gasFile,
                                           String year,
                                           String monthRangeRaw) {

        // 소스명만 월범위를 사용 (데이터는 '계' 컬럼만 읽음)
        String monthRange = monthRangeRaw.replace("~", "-").trim();
        int[] months = parseMonthRange(monthRange);

        log.info("[IMPORT] year={}, monthRange={}, months={}",
                year, monthRange, Arrays.toString(months));
        log.info("[IMPORT] elecFile={}, gasFile={}",
                elecFile != null ? elecFile.getOriginalFilename() : "null",
                gasFile  != null ? gasFile.getOriginalFilename()  : "null");

        Map<String, BigDecimal> elecByRegion = (elecFile != null) ? parseSheetTotalOnly(elecFile) : Map.of();
        Map<String, BigDecimal> gasByRegion  = (gasFile  != null) ? parseSheetTotalOnly(gasFile)  : Map.of();

        log.info("[IMPORT] parsed ELEC regions={}, GAS regions={}", elecByRegion.size(), gasByRegion.size());

        String elecSource = "ELEC_M" + months[0] + "_" + months[months.length - 1];
        String gasSource  = "GAS_M"  + months[0] + "_" + months[months.length - 1];

        int inserted = 0, updated = 0, skipped = 0;

        // 전기
        for (var e : elecByRegion.entrySet()) {
            Region region = findRegionByNameOrCode(e.getKey());
            if (region == null) { skipped++; continue; }
            if (upsert(region, year, elecSource, e.getValue())) updated++; else inserted++;
        }
        // 가스
        for (var e : gasByRegion.entrySet()) {
            Region region = findRegionByNameOrCode(e.getKey());
            if (region == null) { skipped++; continue; }
            if (upsert(region, year, gasSource, e.getValue())) updated++; else inserted++;
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("year", year);
        res.put("monthRange", monthRange);
        res.put("inserted", inserted);
        res.put("updated", updated);
        res.put("skipped", skipped);
        res.put("sources", List.of(elecSource, gasSource));

        log.info("[IMPORT] done: {}", res);
        return res;
    }

    // 소스명(ELEC_Ms_e) 생성을 위한 월범위 파싱
    private int[] parseMonthRange(String monthRange) {
        try {
            String[] a = monthRange.split("-");
            int s = Integer.parseInt(a[0].trim());
            int t = Integer.parseInt(a[1].trim());
            if (s <= 0 || t <= 0 || t < s || t > 12) throw new IllegalArgumentException();
            return java.util.stream.IntStream.rangeClosed(s, t).toArray();
        } catch (Exception e) {
            throw new IllegalArgumentException("monthRange 형식은 예: 1-5");
        }
    }

    //자치구+계
    private Map<String, BigDecimal> parseSheetTotalOnly(MultipartFile file) {
        Map<String, BigDecimal> out = new HashMap<>();
        try (InputStream in = file.getInputStream()) {
            Workbook wb = WorkbookFactory.create(in);
            FormulaEvaluator eval = wb.getCreationHelper().createFormulaEvaluator();
            Sheet sh = pickSheet(wb, eval);

            int headerRowIdx = detectHeaderRowByRegionAndTotal(sh, eval);
            Row header = sh.getRow(headerRowIdx);

            int regionCol = -1;
            int totalCol  = -1;

            int lastCol = Math.max(25, header.getLastCellNum()); // 안전 버퍼
            for (int cIdx = 0; cIdx < lastCol; cIdx++) {
                String h = normalize(readHeaderText(sh, headerRowIdx, cIdx, eval));
                if (isRegionHeader(h)) regionCol = cIdx;
                if (isTotalHeader(h))  totalCol  = cIdx;
            }

            log.info("[PARSE] file={} headerRowIdx={} regionCol={} totalCol={}",
                    file.getOriginalFilename(), headerRowIdx, regionCol, totalCol);

            if (regionCol < 0 || totalCol < 0) {
                throw new IllegalStateException("엑셀 헤더 매핑 실패 (자치구 or 계)");
            }

            for (int r = headerRowIdx + 1; r <= sh.getLastRowNum(); r++) {
                Row row = sh.getRow(r);
                if (row == null) continue;

                String regionRaw  = DF.formatCellValue(row.getCell(regionCol), eval);
                String regionNorm = normalize(regionRaw);
                // 전체합 행/빈 행 스킵
                if (regionNorm.isEmpty() || "합계".equals(regionNorm) || "총합".equals(regionNorm)) continue;

                String regionKey = sanitizeRegionKey(regionNorm);
                BigDecimal total = getNumber(row.getCell(totalCol), eval);

                // 단위 보정(kg→t) + 소수2자리 반올림
                total = total.divide(DIV_1000, 6, java.math.RoundingMode.HALF_UP)
                        .setScale(2, java.math.RoundingMode.HALF_UP);

                out.put(regionKey, total);
            }
        } catch (Exception ex) {
            throw new RuntimeException("엑셀 파싱 실패: " + ex.getMessage(), ex);
        }
        return out;
    }

    //자치구+계에 존재하는 행
    private int detectHeaderRowByRegionAndTotal(Sheet sh, FormulaEvaluator eval) {
        int maxProbe = Math.min(10, sh.getLastRowNum());
        for (int r = sh.getFirstRowNum(); r <= maxProbe; r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;

            boolean foundRegion = false, foundTotal = false;
            int lastCol = Math.max(25, row.getLastCellNum());

            for (int cIdx = 0; cIdx < lastCol; cIdx++) {
                String h = normalize(readHeaderText(sh, r, cIdx, eval));
                if (!foundRegion && isRegionHeader(h)) foundRegion = true;
                if (!foundTotal  && isTotalHeader(h))  foundTotal  = true;
                if (foundRegion && foundTotal) return r;
            }
        }
        throw new IllegalStateException("엑셀 헤더(자치구/계) 매칭 실패");
    }

    //시트명 오타 최소화
    private Sheet pickSheet(Workbook wb, FormulaEvaluator eval) {
        String[] candidates = { "에너지 사용량 통계", "에너지 사옹량 통계", "온실가스 배출량 통계" };
        for (String name : candidates) {
            Sheet s = wb.getSheet(name);
            if (s != null && canDetectHeaderRegionTotal(s, eval)) return s;
        }
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet s = wb.getSheetAt(i);
            if (canDetectHeaderRegionTotal(s, eval)) return s;
        }
        throw new IllegalStateException("적절한 시트를 찾지 못했습니다.");
    }
    private boolean canDetectHeaderRegionTotal(Sheet sh, FormulaEvaluator eval) {
        try { detectHeaderRowByRegionAndTotal(sh, eval); return true; }
        catch (Exception e) { return false; }
    }

    //병합, 멀티헤더 보정
    private String readHeaderText(Sheet sh, int rowIdx, int colIdx, FormulaEvaluator eval) {
        String v = DF.formatCellValue(getCellSafe(sh, rowIdx, colIdx), eval);
        if (v != null && !v.isBlank()) return v;

        for (var mr : sh.getMergedRegions()) {
            if (mr.isInRange(rowIdx, colIdx)) {
                var topLeft = getCellSafe(sh, mr.getFirstRow(), mr.getFirstColumn());
                String t = DF.formatCellValue(topLeft, eval);
                if (t != null && !t.isBlank()) return t;
            }
        }
        for (int d : new int[]{-1, 1, -2, 2}) {
            int rr = rowIdx + d;
            if (rr < sh.getFirstRowNum() || rr > sh.getLastRowNum()) continue;

            String adj = DF.formatCellValue(getCellSafe(sh, rr, colIdx), eval);
            if (adj != null && !adj.isBlank()) return adj;

            for (var mr : sh.getMergedRegions()) {
                if (mr.isInRange(rr, colIdx)) {
                    var topLeft = getCellSafe(sh, mr.getFirstRow(), mr.getFirstColumn());
                    String t = DF.formatCellValue(topLeft, eval);
                    if (t != null && !t.isBlank()) return t;
                }
            }
        }
        return "";
    }

    private Cell getCellSafe(Sheet sh, int rowIdx, int colIdx) {
        Row r = sh.getRow(rowIdx);
        return (r == null) ? null : r.getCell(colIdx);
    }

    private boolean isRegionHeader(String s) {
        if (s == null || s.isEmpty()) return false;
        for (String cand : REGION_HEADER_CANDIDATES) {
            if (s.contains(cand)) return true;
        }
        return false;
    }

    private boolean isTotalHeader(String s) {
        String ns = normalize(s);
        return ns.equals("계") || ns.equals("합계") || ns.equals("총합") || ns.equalsIgnoreCase("total");
    }

    private String sanitizeRegionKey(String key) {
        if (key == null) return "";
        String k = key.trim();
        if (k.endsWith("구청")) k = k.substring(0, k.length() - 1);
        if (!k.endsWith("구") && k.length() <= 3) k = k + "구";
        return k;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\s\\u00A0]", "").trim();
    }

    /** 수식/문자/숫자 안전 파싱 */
    private BigDecimal getNumber(Cell c, FormulaEvaluator eval) {
        if (c == null) return BigDecimal.ZERO;
        switch (c.getCellType()) {
            case NUMERIC: return BigDecimal.valueOf(c.getNumericCellValue());
            case STRING:  return parseDecimal(DF.formatCellValue(c, eval));
            case FORMULA:
                CellValue cv = eval.evaluate(c);
                if (cv == null) return BigDecimal.ZERO;
                switch (cv.getCellType()) {
                    case NUMERIC: return BigDecimal.valueOf(cv.getNumberValue());
                    case STRING:  return parseDecimal(cv.getStringValue());
                    default:      return BigDecimal.ZERO;
                }
            default:
                return parseDecimal(DF.formatCellValue(c, eval));
        }
    }
    private BigDecimal parseDecimal(String s) {
        if (s == null) return BigDecimal.ZERO;
        String t = s.replace(",", "").trim();
        if (t.isEmpty()) return BigDecimal.ZERO;
        try { return new BigDecimal(t); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private Region findRegionByNameOrCode(String key) {
        return regionRepo.findById(key)
                .orElseGet(() -> regionRepo.findByName(key).orElse(null));
    }

    private boolean upsert(Region region, String year, String source, BigDecimal value) {
        return ceRepo.findByRegionAndYearAndSource(region, year, source)
                .map(ex -> {
                    ceRepo.delete(ex);
                    ceRepo.save(newEmission(region, year, source, value));
                    return true;
                })
                .orElseGet(() -> {
                    ceRepo.save(newEmission(region, year, source, value));
                    return false;
                });
    }

    private CarbonEmission newEmission(Region region, String year, String source, BigDecimal value) {
        try {
            CarbonEmission ce = new CarbonEmission();
            set(ce, "year", year);
            set(ce, "source", source);
            set(ce, "unit", "tCO2e");
            set(ce, "versionDate", LocalDate.now());
            set(ce, "totalTCO2", value); // 1/1000 적용 & 소수2자리 반올림 후 값
            set(ce, "region", region);
            return ce;
        } catch (Exception e) {
            throw new RuntimeException("CarbonEmission 값 설정 실패: " + e.getMessage());
        }
    }

    private static void set(Object target, String field, Object v) throws Exception {
        var f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, v);
    }
}
