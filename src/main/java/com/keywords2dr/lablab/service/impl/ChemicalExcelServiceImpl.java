package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.chemical.ChemicalRequestDTO;
import com.keywords2dr.lablab.entity.Chemical;
import com.keywords2dr.lablab.repository.ChemicalRepository;
import com.keywords2dr.lablab.service.AuditLogService;
import com.keywords2dr.lablab.service.ChemicalExcelService;
import com.keywords2dr.lablab.service.ChemicalService;
import com.keywords2dr.lablab.service.DataNormalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChemicalExcelServiceImpl implements ChemicalExcelService {

    private final ChemicalRepository chemicalRepository;
    private final DataNormalizationService normalizationService;
    private final ChemicalService chemicalService;
    private final AuditLogService auditLogService;

    @Override
    public Map<String, Object> processAndSaveImport(MultipartFile file) {
        List<ChemicalRequestDTO> dtoList = this.parseChemicalsFromExcel(file);

        int successCount = 0;
        List<Map<String, String>> failures = new ArrayList<>();

        for (ChemicalRequestDTO dto : dtoList) {
            try {
                chemicalService.createChemical(dto);
                successCount++;
            } catch (Exception e) {
                log.warn("❌ Bỏ qua hóa chất [{}] do lỗi: {}", dto.getItemCode(), e.getMessage());
                failures.add(Map.of(
                        "itemCode", dto.getItemCode() != null ? dto.getItemCode() : "(không có mã)",
                        "name",     dto.getName()     != null ? dto.getName()     : "(không có tên)",
                        "reason",   e.getMessage()
                ));
            }
        }

        Map<String, Object> importSummary = new HashMap<>();
        importSummary.put("fileName",        file.getOriginalFilename());
        importSummary.put("totalRowsParsed", dtoList.size());
        importSummary.put("successCount",    successCount);
        importSummary.put("failCount",       failures.size());

        auditLogService.logAction("IMPORT_EXCEL", "CHEMICAL", null, null, importSummary);

        Map<String, Object> response = new HashMap<>();
        response.put("message",      String.format("Import hoàn tất! Thành công: %d. Thất bại (bỏ qua): %d", successCount, failures.size()));
        response.put("successCount", successCount);
        response.put("failCount",    failures.size());
        if (!failures.isEmpty()) {
            response.put("failures", failures);
        }
        return response;
    }

    @Override
    public List<ChemicalRequestDTO> parseChemicalsFromExcel(MultipartFile file) {
        if (!isExcelFormat(file)) throw new RuntimeException("Vui lòng tải lên file Excel (.xlsx)!");

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = findDataSheet(workbook);
            Iterator<Row> rows = sheet.iterator();
            List<ChemicalRequestDTO> chemicals = new ArrayList<>();

            Map<String, Integer> columnMap = new HashMap<>();
            boolean foundHeader = false;

            while (rows.hasNext()) {
                Row currentRow = rows.next();

                if (!foundHeader) {
                    if (isHeaderRow(currentRow, columnMap)) {
                        foundHeader = true;
                    }
                    continue;
                }

                if (isRowEmpty(currentRow)) continue;

                ChemicalRequestDTO dto = new ChemicalRequestDTO();

                String name = getCellData(currentRow, columnMap, "TÊN HÓA CHẤT", "TEN");
                if (name == null || name.isEmpty()) continue;

                dto.setName(name);
                dto.setFormula(getCellData(currentRow, columnMap, "CÔNG THỨC"));
                dto.setSupplier(getCellData(currentRow, columnMap, "NHÀ CUNG CẤP"));

                dto.setPackaging(normalizationService.normalizeAndLearn(getCellData(currentRow, columnMap, "ĐÓNG GÓI"), "PACKAGING"));
                dto.setUnit(normalizationService.normalizeAndLearn(getCellData(currentRow, columnMap, "ML/L/KG/G", "ĐƠN VỊ"), "UNIT"));

                String amountStr = getCellData(currentRow, columnMap, "KHỐI LƯỢNG", "DUNG TÍCH");
                if (amountStr != null && !amountStr.trim().isEmpty()) {
                    try {
                        dto.setAmountPerPackage(new BigDecimal(amountStr));
                    } catch (NumberFormatException ignored) {}
                }

                String itemCode = getCellData(currentRow, columnMap, "MÃ HÓA CHẤT");
                if (itemCode == null || itemCode.isEmpty()) {
                    itemCode = "HC_AUTO_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                }
                dto.setItemCode(itemCode);

                chemicals.add(dto);
            }

            if (chemicals.isEmpty()) throw new RuntimeException("Không tìm thấy dữ liệu hóa chất hợp lệ trong file!");
            return chemicals;

        } catch (IOException e) {
            log.error("Lỗi khi đọc file Excel: ", e);
            throw new RuntimeException("Lỗi đọc file: " + e.getMessage());
        }
    }

    @Override
    public ByteArrayInputStream exportChemicalsToExcel() {
        List<Chemical> chemicals = chemicalRepository.findAllByIsDeletedFalse();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Kho_Hoa_Chat");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle   = createDataStyle(workbook);

            String[] headers = {"STT", "Mã Hóa Chất", "Tên Hóa Chất", "Công Thức",
                    "Đơn Vị Tính", "Quy Cách", "Dung Tích/Gói", "Nhà Cung Cấp"};

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(25);
            for (int col = 0; col < headers.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Chemical c : chemicals) {
                Row row = sheet.createRow(rowIdx);
                createCell(row, 0, String.valueOf(rowIdx), dataStyle);
                createCell(row, 1, c.getItemCode(), dataStyle);
                createCell(row, 2, c.getName(), dataStyle);
                createCell(row, 3, c.getFormula(), dataStyle);
                createCell(row, 4, c.getUnit(), dataStyle);
                createCell(row, 5, c.getPackaging(), dataStyle);

                Cell amountCell = row.createCell(6);
                amountCell.setCellStyle(dataStyle);
                if (c.getAmountPerPackage() != null) {
                    amountCell.setCellValue(c.getAmountPerPackage().doubleValue());
                } else {
                    amountCell.setCellValue("");
                }

                createCell(row, 7, c.getSupplier(), dataStyle);
                rowIdx++;
            }

            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, rowIdx - 1, 0, headers.length - 1));
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            log.error("Lỗi xuất Excel: ", e);
            throw new RuntimeException("Lỗi hệ thống khi xuất file Excel!");
        }
    }

    // --- CÁC HÀM HỖ TRỢ ---

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        style.setFont(font);

        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
    private Sheet findDataSheet(Workbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            String sheetName = sheet.getSheetName().toLowerCase();
            if (sheetName.contains("tổng hợp") || sheetName.contains("khtn") || sheetName.contains("data")) {
                return sheet;
            }
        }
        return workbook.getSheetAt(0);
    }

    private boolean isHeaderRow(Row row, Map<String, Integer> columnMap) {
        int matchCount = 0;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            String cellValue = getCellValueAsString(row.getCell(c));
            if (cellValue != null && !cellValue.isEmpty()) {
                String normalizedHeader = cellValue.toUpperCase().trim();
                columnMap.put(normalizedHeader, c);

                if (normalizedHeader.contains("TÊN") || normalizedHeader.contains("CÔNG THỨC") || normalizedHeader.contains("STT")) {
                    matchCount++;
                }
            }
        }
        return matchCount >= 2;
    }

    private String getCellData(Row row, Map<String, Integer> columnMap, String... possibleHeaders) {
        for (String headerMatch : possibleHeaders) {
            for (Map.Entry<String, Integer> entry : columnMap.entrySet()) {
                if (entry.getKey().contains(headerMatch)) {
                    return getCellValueAsString(row.getCell(entry.getValue()));
                }
            }
        }
        return null;
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private boolean isExcelFormat(MultipartFile file) {
        String contentType = file.getContentType();
        String filename    = file.getOriginalFilename();
        boolean validType  = contentType != null && contentType.contains("spreadsheetml");
        boolean validExt   = filename != null && filename.toLowerCase().endsWith(".xlsx");
        return validType || validExt;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            default -> "";
        };
    }
}