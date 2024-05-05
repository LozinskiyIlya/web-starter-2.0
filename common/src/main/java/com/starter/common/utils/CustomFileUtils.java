package com.starter.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


@Slf4j
public class CustomFileUtils {

    public static String downloadFileFromUrl(String fileUrl, String outputFileName) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            Resource resource = restTemplate.getForObject(fileUrl, Resource.class);
            File output = new File(outputFileName);
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
        File file = new File(filePath);
        if (file.delete()) {
            log.info("File deleted successfully: " + filePath);
        } else {
            log.info("Failed to delete the file: " + filePath);
        }
    }

}
