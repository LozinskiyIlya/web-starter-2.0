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

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Disabled
public class OpenAiAssistantIT {

    @Autowired
    private OpenAiAssistant openAiAssistant;

    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    @Disabled
    @DisplayName("Calls chat completions api")
    void callsChatCompletionsApi() {
        String response = openAiAssistant.chatCompletion("Some system message, ignore it", "Hello, my name is John and I am a");
        System.out.println(response);
    }

    @Test
    @Disabled
    @DisplayName("Classifies message")
    void classifiesMessage() {
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
        assertEquals(2026.5, response.getAmount());
        assertEquals("EUR", response.getCurrency());
        System.out.println(response);
    }

    @Test
    @Disabled
    @DisplayName("Runs text pipeline with default currency")
    void runsTextPipeWithDefaultCurrency() {
        final var message = "Байк 700К";
        final var defaultCurrency = "IDR";
        final var response = openAiAssistant.runTextPipeline(UUID.randomUUID(), message, defaultCurrency);
        assertEquals(defaultCurrency, response.getCurrency());
        System.out.println(response);
    }

    @Test
    @Disabled
    @DisplayName("Default currency override")
    void defaultCurrencyOverride() {
        final var message = "Байк 700К RUB";
        final var defaultCurrency = "IDR";
        final var response = openAiAssistant.runTextPipeline(UUID.randomUUID(), message, defaultCurrency);
        assertEquals("RUB", response.getCurrency());
        System.out.println(response);
    }

    abstract class RunFilePipeline {

        protected Supplier<String> fileUrl;
        protected Supplier<Double> expectedAmount;
        protected Supplier<String> expectedCurrency;

        @SneakyThrows
        @Disabled
        @Test
        @DisplayName("For some file extension")
        void forSomeFileExtension() {
            final var additionalMessage = "Sending you an invoice for the last tasks";
            final var response = openAiAssistant.runFilePipeline(UUID.randomUUID(), fileUrl.get(), additionalMessage, null);
            assertEquals(expectedAmount.get(), response.getAmount());
            assertEquals(expectedCurrency.get(), response.getCurrency());
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
            expectedAmount = () -> 9100d;
            expectedCurrency = () -> "EUR";
        }
    }

    @Nested
    @DisplayName("For JPG files")
    class RunJPGFilePipeline extends RunFilePipeline {
        {
            fileUrl = () -> "https://volee-avatars-dev-us.s3.amazonaws.com/ai-counting/Invoice1.jpg";
            expectedAmount = () -> 154500d;
            expectedCurrency = () -> "IDR";
        }
    }

    @Nested
    @DisplayName("For PNG files")
    class RunPNGFilePipeline extends RunFilePipeline {
        {
            fileUrl = () -> "https://volee-avatars-dev-us.s3.amazonaws.com/ai-counting/Invoice2.png";
            expectedAmount = () -> 9100d;
            expectedCurrency = () -> "EUR";
        }
    }
}
