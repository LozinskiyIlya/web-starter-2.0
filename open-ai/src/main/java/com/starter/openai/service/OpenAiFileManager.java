package com.starter.openai.service;


import com.theokanning.openai.file.File;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiFileManager {

    private static final String FILE_PURPOSE = "assistants";

    private final OpenAiService openAiService;

    public File uploadFile(String filePath) {
        final var uploaded = openAiService.uploadFile(FILE_PURPOSE, filePath);
        log.info("Uploaded file status: {}, details: {}", uploaded.getStatus(), uploaded.getStatusDetails());
        return uploaded;
    }
}
