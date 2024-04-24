package com.starter.web.service.openai;


import com.theokanning.openai.service.OpenAiService;
import kotlin.Pair;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiFileManager {

    private static final String FILE_PURPOSE = "assistants";

    private final OpenAiService openAiService;

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
            case "jpg", "jpeg", "png" -> new Pair<>(imgToPdf(filePath, fileName), true);
            default -> throw new IllegalArgumentException("Unsupported file format: " + fileFormat);
        };
    }

    @SneakyThrows(IOException.class)
    private static String imgToPdf(String filePath, String fileName) {
        // Load the image
        final File file = new File(filePath);
        final var loaded = ImageIO.read(file);
        // Create a new PDF document
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(loaded.getWidth(), loaded.getHeight()));
            doc.addPage(page);
            // Create a content stream
            try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
                // Create an image object from the loaded image
                final var pdImage = LosslessFactory.createFromImage(doc, loaded);
                // Draw the image onto the page
                contentStream.drawImage(pdImage, 0, 0, loaded.getWidth(), loaded.getHeight());
            }
            // Save the document
            doc.save(fileName + ".pdf");
        }
        return fileName + ".pdf";
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
