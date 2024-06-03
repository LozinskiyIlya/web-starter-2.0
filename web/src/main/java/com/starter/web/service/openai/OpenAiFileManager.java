package com.starter.web.service.openai;


import com.theokanning.openai.service.OpenAiService;
import kotlin.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiFileManager {

    private static final String FILE_PURPOSE = "assistants";

    private final OpenAiService openAiService;
    private final ImgToTextTransformer imgToTextTransformer;

    public com.theokanning.openai.file.File uploadFile(String filePath) {
        final var processedFilePath = preprocessFile(filePath);
        final var uploaded = openAiService.uploadFile(FILE_PURPOSE, processedFilePath.getFirst());
        log.info("Uploaded file status: {}, details: {}", uploaded.getStatus(), uploaded.getStatusDetails());
        postProcessFile(processedFilePath);
        return uploaded;
    }

    private Pair<String, Boolean> preprocessFile(String filePath) {
        final var fileFormat = filePath.substring(filePath.lastIndexOf('.') + 1);
        return switch (fileFormat) {
            case "pdf" -> new Pair<>(filePath, false);
            case "jpg", "jpeg", "png" -> new Pair<>(imgToTextTransformer.transformAndGetPath(filePath, fileFormat), true);
            default -> throw new IllegalArgumentException("Unsupported file format: " + fileFormat);
        };
    }

    private void postProcessFile(Pair<String, Boolean> processedFilePath) {
        final var needToDelete = processedFilePath.getSecond();
        if (needToDelete) {
            delete(processedFilePath);
        }
    }

    private static void delete(Pair<String, Boolean> processedFilePath) {
        try {
            final var deleted = new File(processedFilePath.getFirst()).delete();
            log.info("File delete status : {}", deleted);
        } catch (SecurityException e) {
            log.error("SecurityException occurred while deleting file: {}", processedFilePath.getFirst(), e);
        } catch (Exception e) {
            log.error("Exception occurred while deleting file: {}", processedFilePath.getFirst(), e);
        }
    }

    public void deleteFile(String fileId) {
        final var deleteResult = openAiService.deleteFile(fileId);
        log.info("File delete result: {}", deleteResult);
        // todo delete vector storage
    }
}
