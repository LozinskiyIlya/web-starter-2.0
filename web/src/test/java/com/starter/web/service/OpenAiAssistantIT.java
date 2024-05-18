package com.starter.web.service;


import com.starter.web.fragments.MessageClassificationResponse;
import com.starter.web.service.openai.OpenAiAssistant;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import java.util.function.Supplier;

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

                Тогда отправим 2026.5 EUR""";
        final var response = openAiAssistant.runTextPipeline(UUID.randomUUID(), message, null);
        System.out.println(response);
    }

    @Test
    @Disabled
    @DisplayName("Runs text pipeline with default currency")
    void runsTextPipeWithDefaultCurrency() {
        final var message = "Байк 700К";
        final var defaultCurrency = "IDR";
        final var response = openAiAssistant.runTextPipeline(UUID.randomUUID(), message, defaultCurrency);
        System.out.println(response);
    }

    @Test
    @Disabled
    @DisplayName("Default currency override")
    void defaultCurrencyOverride() {
        final var message = "Байк 700К RUB";
        final var defaultCurrency = "IDR";
        final var response = openAiAssistant.runTextPipeline(UUID.randomUUID(), message, defaultCurrency);
        System.out.println(response);
    }

    abstract class RunFilePipeline {

        protected Supplier<String> fileUrl;

        @SneakyThrows
        @Disabled
        @Test
        @DisplayName("For some file extension")
        void forSomeFileExtension() {
            final var additionalMessage = "Sending you an invoice for the last tasks";
            final var response = openAiAssistant.runFilePipeline(UUID.randomUUID(), fileUrl.get(), additionalMessage, null);
            System.out.println(response);
        }
    }

    @Nested
    @DisplayName("For PDF files")
    class RunPDFFilePipeline extends RunFilePipeline {
        {
            final var path = "files/pdf/Invoice2.pdf";
            final var resource = resourceLoader.getResource("classpath:" + path);
            final URL url;
            try {
                url = resource.getURL();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            fileUrl = url::getPath;
        }
    }

    @Nested
    @DisplayName("For PNG files")
    class RunPNGFilePipeline extends RunFilePipeline {
        {
            fileUrl = () -> "https://api.telegram.org/file/bot7126952763:AAH5WcT1TPGBS53WIGYNASsgao8D2UnhRR8/photos/file_0.jpg";
        }
    }
}
