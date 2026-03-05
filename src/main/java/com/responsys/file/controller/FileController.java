package com.responsys.file.controller;

import com.responsys.file.dto.BatchUploadResponse;
import com.responsys.file.service.FileProcessingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileProcessingService fileProcessingService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");
        String token = StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7) : "";

        BatchUploadResponse response = fileProcessingService.processFile(file, token);
        return ResponseEntity.ok(response);
    }
}
