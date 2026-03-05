package com.responsys.file.parser;

import com.responsys.file.dto.ContactRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

@Component
@Slf4j
public class ExcelParser implements FileParser {

    @Override
    public boolean supports(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".xls") || lower.endsWith(".xlsx");
    }

    @Override
    public List<ContactRecord> parse(InputStream inputStream, String filename, List<String> parseErrors) throws IOException {
        List<ContactRecord> records = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                parseErrors.add("La hoja de cálculo está vacía");
                return records;
            }

            // Fila 0 = headers
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> headerIndex = new HashMap<>();
            for (Cell cell : headerRow) {
                headerIndex.put(cell.getStringCellValue().trim().toLowerCase(), cell.getColumnIndex());
            }

            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;

                try {
                    ContactRecord record = ContactRecord.builder()
                            .email(getCellValue(row, headerIndex, "email"))
                            .firstName(getCellValue(row, headerIndex, "firstname"))
                            .lastName(getCellValue(row, headerIndex, "lastname"))
                            .phone(getCellValue(row, headerIndex, "phone"))
                            .status(getCellValue(row, headerIndex, "status"))
                            .sourceFile(filename)
                            .build();

                    if (record.getEmail() == null || record.getEmail().isBlank()) {
                        parseErrors.add("Fila " + (rowNum + 1) + ": email vacío, se omite");
                        continue;
                    }
                    records.add(record);
                } catch (Exception e) {
                    parseErrors.add("Fila " + (rowNum + 1) + ": error al parsear - " + e.getMessage());
                    log.warn("Error parseando fila {} del archivo {}: {}", rowNum + 1, filename, e.getMessage());
                }
            }
        }

        log.debug("ExcelParser: {} registros parseados de {}", records.size(), filename);
        return records;
    }

    private String getCellValue(Row row, Map<String, Integer> headerIndex, String header) {
        Integer colIdx = headerIndex.get(header);
        if (colIdx == null) return null;
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) yield cell.getDateCellValue().toString();
                // Evitar notacion cientifica en numeros largos (ej. telefono)
                yield String.valueOf((long) cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.STRING
                    ? cell.getStringCellValue()
                    : String.valueOf((long) cell.getNumericCellValue());
            default -> null;
        };
    }
}
