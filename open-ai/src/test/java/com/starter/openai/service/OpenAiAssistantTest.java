package com.starter.openai.service;


import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.testcontainers.shaded.com.google.common.base.Charsets;
import org.testcontainers.shaded.com.google.common.io.ByteSource;

import java.io.InputStream;
import java.util.UUID;

@SpringBootTest
public class OpenAiAssistantTest {

    @Autowired
    private OpenAiAssistant openAiAssistant;

    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    @Disabled
    @DisplayName("Calls completions api")
    void callsCompletionsApi() {
        String response = openAiAssistant.completion("Hello, my name is John and I am a");
        System.out.println(response);
    }

    @Test
    @Disabled
    @DisplayName("Calls chat completions api")
    void callsChatCompletionsApi() {
        String response = openAiAssistant.chatCompletion("Hello, my name is John and I am a student.");
        System.out.println(response);
    }

    @SneakyThrows
    @Test
//    @Disabled
    @DisplayName("Run file pipeline")
    void runFilePipe() {
        final var path = "files/pdf/Invoice2.pdf";
        var resource = resourceLoader.getResource("classpath:" + path);
        String response = openAiAssistant.runFilePipeline(resource.getFile().getAbsolutePath(), UUID.randomUUID());
        System.out.println(response);
    }

    @SneakyThrows
    @Test
//    @Disabled
    @DisplayName("Run text pipeline")
    void runTextPipe() {
        final var message = "#money Nov 2023 (спринты 11-12)\n" +
                "\n" +
                "72.5*30 = 2175 USD = 1996 EUR \n" +
                "\n" +
                "Был долг #Долг 0 EUR \n" +
                "\n" +
                "Долг полный: 1996 EUR \n" +
                "\n" +
                "Плюс эти банк комиссии сколько обычно? вроде 30 EUR ? \n" +
                "\n" +
                "Тогда отправим 2026 EUR";
        final var response = openAiAssistant.runTextPipeline(message, UUID.randomUUID());
        System.out.println(response);
    }


    @SneakyThrows
    public static String readResource(String filename) {
        var template = new ClassPathResource(filename);
        var text = "";
        try (InputStream html = template.getInputStream()) {
            ByteSource byteSource = new ByteSource() {
                @Override
                public InputStream openStream() {
                    return html;
                }
            };
            text = byteSource.asCharSource(Charsets.UTF_8).read();
        }
        return text;
    }
}
