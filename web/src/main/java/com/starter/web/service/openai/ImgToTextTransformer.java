package com.starter.web.service.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starter.common.service.HttpService;
import com.starter.web.fragments.OcrResponse;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.starter.web.service.openai.OpenAiAssistant.MAX_TOKENS;
import static com.starter.web.service.openai.OpenAiAssistant.VISION_MODEL;
import static com.starter.web.service.openai.StaticPromptRenderer.VISION_PROMPT;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImgToTextTransformer {
    public static final String VISION_MESSAGE_ROLE = "vision";

    @Value("${starter.ocr.api-key}")
    private String OCR_API_KEY;
    private static final String OCR_API_URL = "https://api.ocr.space/parse/image";

    private final OpenAiService openAiService;
    private final HttpService httpService;
    private final String downloadDirectory;


    @SneakyThrows(IOException.class)
    public String ocrTransformAndGetPath(String filePath, String fileFormat) {
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
        final var outputFilePath = Paths.get(downloadDirectory, System.currentTimeMillis() + ".txt");
        Files.write(outputFilePath, extractedText.getBytes());

        // Return the path to the created file
        return outputFilePath.toString();
    }

    @SneakyThrows(IOException.class)
    public String visionTransformAndGetPath(String imageUrl) {
        final var response = openAiService.createChatCompletion(ChatCompletionRequest.builder()
                .maxTokens(MAX_TOKENS)
                .model(VISION_MODEL)
                .n(1)
                .messages(List.of(
                        new ChatMessage(
                                VISION_MESSAGE_ROLE,
                                getVisionContent(VISION_PROMPT, imageUrl)
                        )))
                .build());
        final var textOnImage = response.getChoices().get(0).getMessage().getContent();
        final var outputFilePath = Paths.get(downloadDirectory, System.currentTimeMillis() + ".txt");
        Files.write(outputFilePath, textOnImage.getBytes());
        // Return the path to the created file
        return outputFilePath.toString();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class VisionMessageContent {
        private String type;
        private String text;
        @JsonProperty("image_url")
        private ImageUrl imageUrl;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class ImageUrl {
        private String url;
        private String detail;
    }

    @SneakyThrows
    private String getVisionContent(String caption, String imageUrl) {
        final var list = List.of(
                new VisionMessageContent("text", caption, null),
                new VisionMessageContent("image_url", null, new ImageUrl(imageUrl, "auto"))
        );
        final var mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.valueToTree(list).toString();
    }

}
