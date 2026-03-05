package com.responsys.file.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BatchUploadResponse {
    private int totalRecords;
    private int savedRecords;
    private List<String> errors;
    private String message;
}
