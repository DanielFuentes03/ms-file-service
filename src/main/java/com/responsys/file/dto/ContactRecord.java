package com.responsys.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactRecord {
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String status;
    private String sourceFile;
}
