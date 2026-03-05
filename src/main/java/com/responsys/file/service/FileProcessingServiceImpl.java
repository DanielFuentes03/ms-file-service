package com.responsys.file.service;

import com.responsys.file.dto.BatchUploadResponse;
import com.responsys.file.dto.ContactRecord;
import com.responsys.file.exception.FileParseException;
import com.responsys.file.parser.FileParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileProcessingServiceImpl implements FileProcessingService {

    private final List<FileParser> parsers;
    private final RestTemplate restTemplate;

    @Value("${services.data-service.url}")
    private String dataServiceUrl;

    @Override
    public BatchUploadResponse processFile(MultipartFile file, String jwtToken) {
        String filename = file.getOriginalFilename();
        List<String> parseErrors = new ArrayList<>();

        // Seleccionar parser
        FileParser parser = parsers.stream()
                .filter(p -> p.supports(filename))
                .findFirst()
                .orElseThrow(() -> new FileParseException("Formato de archivo no soportado: " + filename));

        // Parsear
        List<ContactRecord> records;
        try {
            records = parser.parse(file.getInputStream(), filename, parseErrors);
        } catch (Exception e) {
            log.error("Error al parsear el archivo {}: {}", filename, e.getMessage());
            throw new FileParseException("Error al procesar el archivo: " + e.getMessage());
        }

        if (records.isEmpty()) {
            return new BatchUploadResponse(0, 0, parseErrors,
                    "No se encontraron registros válidos en el archivo");
        }

        // Enviar al data-service
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + jwtToken);
        HttpEntity<List<ContactRecord>> entity = new HttpEntity<>(records, headers);

        int saved = 0;
        try {
            ResponseEntity<List<?>> response = restTemplate.exchange(
                    dataServiceUrl + "/api/records/batch",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<?>>() {}
            );
            if (response.getBody() != null) {
                saved = response.getBody().size();
            }
        } catch (Exception e) {
            log.error("Error al enviar registros al data-service: {}", e.getMessage());
            parseErrors.add("Error al guardar en la base de datos: " + e.getMessage());
        }

        String message = saved > 0
                ? saved + " de " + records.size() + " registros guardados correctamente"
                : "No se pudieron guardar los registros";

        return new BatchUploadResponse(records.size(), saved, parseErrors, message);
    }
}
