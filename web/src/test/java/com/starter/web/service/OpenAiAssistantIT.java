package com.starter.web.service;


import com.starter.web.fragments.MessageClassificationResponse;
import com.starter.web.service.openai.OpenAiAssistant;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;

import java.util.UUID;

@SpringBootTest
@Disabled
public class OpenAiAssistantIT {

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
        MessageClassificationResponse response = openAiAssistant.classifyMessage("Я расплатился с Username по всем платежам включая последний за Project");
        Assertions.assertTrue(response.isPaymentRelated());

        response = openAiAssistant.classifyMessage("Ну ты спрашиваешь такой вопрос, как будто на него можно по разносу ответить");
        Assertions.assertFalse(response.isPaymentRelated());
    }

    @Test
    @Disabled
    @DisplayName("Runs text pipeline")
    void runsTextPipe() {
        final var message = """
                #money Nov 2023 (спринты 11-12)
                72.5*30 = 2175 USD = 1996 EUR\s

                Был долг #Долг 0 EUR\s
                Долг полный: 1996 EUR\s

                Плюс эти банк комиссии сколько обычно? вроде 30 EUR ?\s

                Тогда отправим 2026 EUR""";
        final var response = openAiAssistant.runTextPipeline(message, UUID.randomUUID());
        System.out.println(response);
    }

    abstract class RunFilePipeline {

        protected abstract String getExtension();

        @SneakyThrows
        @Disabled
        @Test
        @DisplayName("For some file extension")
        void forSomeFileExtension() {
            final var path = String.format("files/%s/Invoice2.%s", getExtension(), getExtension());
            var resource = resourceLoader.getResource("classpath:" + path);
            final var response = openAiAssistant.runFilePipeline(resource.getFile().getAbsolutePath(), UUID.randomUUID());
            System.out.println(response);
        }
    }

    @Nested
    @DisplayName("For PDF files")
    class RunPDFFilePipeline extends RunFilePipeline {

        @Override
        protected String getExtension() {
            return "pdf";
        }
    }

    @Nested
    @DisplayName("For PNG files")
    class RunPNGFilePipeline extends RunFilePipeline {

        @Override
        protected String getExtension() {
            return "png";
        }
    }
}
