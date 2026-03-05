package com.responsys.file.parser;

import com.responsys.file.dto.ContactRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

@Component
@Slf4j
public class TxtPipeParser implements FileParser {

    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".txt");
    }

    @Override
    public List<ContactRecord> parse(InputStream inputStream, String filename, List<String> parseErrors) throws IOException {
        List<ContactRecord> records = new ArrayList<>();
        List<String> lines = new BufferedReader(new InputStreamReader(inputStream))
                .lines().toList();

        if (lines.isEmpty()) {
            parseErrors.add("El archivo está vacío");
            return records;
        }

        // Primera fila = headers (email|firstName|lastName|phone|status)
        String[] headers = lines.get(0).split("\\|");
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            headerIndex.put(headers[i].trim().toLowerCase(), i);
        }

        for (int rowNum = 1; rowNum < lines.size(); rowNum++) {
            String line = lines.get(rowNum).trim();
            if (line.isBlank()) continue;

            String[] fields = line.split("\\|", -1);
            try {
                ContactRecord record = ContactRecord.builder()
                        .email(getField(fields, headerIndex, "email"))
                        .firstName(getField(fields, headerIndex, "firstname"))
                        .lastName(getField(fields, headerIndex, "lastname"))
                        .phone(getField(fields, headerIndex, "phone"))
                        .status(getField(fields, headerIndex, "status"))
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

        log.debug("TxtPipeParser: {} registros parseados de {}", records.size(), filename);
        return records;
    }

    private String getField(String[] fields, Map<String, Integer> headerIndex, String header) {
        Integer idx = headerIndex.get(header);
        if (idx == null || idx >= fields.length) return null;
        return fields[idx].trim();
    }
}
