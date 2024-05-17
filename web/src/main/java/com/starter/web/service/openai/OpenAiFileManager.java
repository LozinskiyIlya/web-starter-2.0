package com.starter.web.service.openai;


import com.starter.common.service.HttpService;
import com.theokanning.openai.service.OpenAiService;
import kotlin.Pair;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiFileManager {

    private static final String FILE_PURPOSE = "assistants";
    private static final String OCR_API_URL = "https://api.ocr.space/parse/image";

    @Value("${starter.ocr.api-key}")
    private String OCR_API_KEY;

    private final OpenAiService openAiService;
    private final HttpService httpService;

    public com.theokanning.openai.file.File uploadFile(String filePath) {
        final var processedFilePath = preprocessFile(filePath);
        final var uploaded = openAiService.uploadFile(FILE_PURPOSE, processedFilePath.getFirst());
        log.info("Uploaded file status: {}, details: {}", uploaded.getStatus(), uploaded.getStatusDetails());
        postProcessFile(processedFilePath);
        return uploaded;
    }

    private Pair<String, Boolean> preprocessFile(String filePath) {
        final var fileName = filePath.substring(0, filePath.lastIndexOf('.'));
        final var fileFormat = filePath.substring(filePath.lastIndexOf('.') + 1);
        return switch (fileFormat) {
            case "pdf" -> new Pair<>(filePath, false);
            case "jpg", "jpeg", "png" -> new Pair<>(imgToText(filePath, fileName), true);
            default -> throw new IllegalArgumentException("Unsupported file format: " + fileFormat);
        };
    }

    @SneakyThrows(IOException.class)
    private String imgToText(String filePath, String fileName) {
        // Load the image
        final var file = new File(filePath);

        // Determine the MIME type of the file
        final var mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null) {
            throw new IOException("Unable to determine MIME type for file: " + filePath);
        }

        // Make a request to OCR.Space
        final var imageFilePath = file.getAbsolutePath();
        final var base64Image = encodeFileToBase64Binary(imageFilePath);
        final var data = "base64Image=" + "data:" + mimeType + ";base64," + base64Image;
        final var headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
        headers.set("apikey", OCR_API_KEY);
        final var extractedText = httpService.postT(OCR_API_URL, data, String.class, headers);
        // Save the extracted text to a file
        final var outputFilePath = fileName + ".txt";
        Files.write(Paths.get(outputFilePath), extractedText.getBytes());

        // Return the path to the created file
        return outputFilePath;
    }

    private static String encodeFileToBase64Binary(String filePath) throws IOException {
        File file = new File(filePath);
        byte[] fileContent = Files.readAllBytes(file.toPath());
        return java.util.Base64.getEncoder().encodeToString(fileContent);
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
