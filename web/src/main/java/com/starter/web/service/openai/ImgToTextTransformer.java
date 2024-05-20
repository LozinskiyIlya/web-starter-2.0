package com.starter.web.service.openai;


import com.starter.common.service.HttpService;
import com.starter.web.fragments.OcrResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImgToTextTransformer {

    @Value("${starter.ocr.api-key}")
    private String OCR_API_KEY;
    private static final String OCR_API_URL = "https://api.ocr.space/parse/image";
    private final HttpService httpService;


    @SneakyThrows(IOException.class)
    public String transformAndGetPath(String filePath, String fileFormat) {
        // Load the image
        final var headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "multipart/form-data");
        headers.set("apikey", OCR_API_KEY);
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("url", filePath);
        builder.part("filetype", fileFormat);
        builder.part("isTable", "true");
        final var ocrResponse = httpService.postT(OCR_API_URL, builder.build(), OcrResponse.class, headers);
        log.info("OCR response: {}", ocrResponse);
        final var extractedText = ocrResponse.getParsedResults()
                .stream()
                .map(OcrResponse.ParsedResult::getParsedText)
                .reduce("", String::concat);
        // Save the extracted text to a file
        final var outputFilePath = System.currentTimeMillis() + ".txt";
        Files.write(Paths.get(outputFilePath), extractedText.getBytes());

        // Return the path to the created file
        return outputFilePath;
    }

}
