package com.responsys.file.service;

import com.responsys.file.dto.BatchUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileProcessingService {
    BatchUploadResponse processFile(MultipartFile file, String jwtToken);
}
