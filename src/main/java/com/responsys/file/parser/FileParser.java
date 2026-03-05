package com.responsys.file.parser;

import com.responsys.file.dto.ContactRecord;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface FileParser {
    boolean supports(String filename);
    List<ContactRecord> parse(InputStream inputStream, String filename, List<String> parseErrors) throws IOException;
}
