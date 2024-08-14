package com.starter.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLConnection;


@Slf4j
public class CustomFileUtils {

    public static String downloadFileFromUrl(String fileUrl, String outputFileName, String outputDirectory) {
        RestTemplate restTemplate = new RestTemplate();
        File output;
        if (StringUtils.hasText(outputDirectory)) {
            output = new File(outputDirectory, outputFileName);
        } else {
            output = new File(outputFileName);
        }
        try {
            Resource resource = restTemplate.getForObject(fileUrl, Resource.class);
            try (InputStream inputStream = resource.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(output)) {
                FileCopyUtils.copy(inputStream, outputStream);
                log.info("File downloaded successfully to: " + output.getAbsolutePath());
                return output.getAbsolutePath();
            } catch (Exception e) {
                log.error("Error while copying file content", e);
            }
        } catch (Exception e) {
            log.error("Failed to download file", e);
        }
        return null;
    }

    public static void deleteLocalFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.delete()) {
                log.info("File deleted successfully from local: " + filePath);
            } else {
                log.error("Failed to delete local the file: " + filePath);
            }
        } catch (Exception e) {
            log.error("Failed to delete local the file: " + filePath, e);
        }
    }

    public static MultipartFile base64ToMultipartFile(String base64, String fileName) {
        try {
            final var contentType = URLConnection.guessContentTypeFromName(fileName);
            final var parts = base64.split(",");
            final var base64Data = parts[1];
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Data);
            return new ByteArrayMultipartFile(decodedBytes, fileName, contentType == null ? "application/octet-stream" : contentType);
        } catch (Exception e) {
            log.error("Failed to convert base64 to MultipartFile", e);
            throw new IllegalArgumentException(e);
        }
    }
}
