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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertTrue(response.isPaymentRelated());

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
        final var response = openAiAssistant.runTextPipeline(message, java.util.Optional.empty(), Set.of());
        assertEquals(2026.5, response.getAmount());
        assertEquals("EUR", response.getCurrency());
        System.out.println(response);
    }


    @Test
    @Disabled
    @DisplayName("Runs text pipeline with custom tags")
    void runsTextPipeWithCustomTags() {
        final var message = """
                Абонемент в спорт-зал 20к рублей за этот месяц
                """;
        final var response = openAiAssistant.runTextPipeline(message, java.util.Optional.empty(), Set.of("Gym", "Party"));
        assertTrue(Arrays.asList(response.getTags()).contains("Gym"));
        System.out.println(response);
    }

    @Test
    @Disabled
    @DisplayName("Runs text pipeline with default currency")
    void runsTextPipeWithDefaultCurrency() {
        final var message = "Байк 700К";
        final var defaultCurrency = "IDR";
        final var response = openAiAssistant.runTextPipeline(message, Optional.of(defaultCurrency), Set.of());
        assertEquals(defaultCurrency, response.getCurrency());
        System.out.println(response);
    }

    @Test
    @Disabled
    @DisplayName("Default currency override")
    void defaultCurrencyOverride() {
        final var message = "Байк 700К RUB";
        final var defaultCurrency = "IDR";
        final var response = openAiAssistant.runTextPipeline(message, Optional.of(defaultCurrency), Set.of());
        assertEquals("RUB", response.getCurrency());
        System.out.println(response);
    }

    @Test
    @Disabled
    @DisplayName("Recognizes current date")
    void shouldRecognizeCurrentDate() {
        final var message = "Байк 700К RUB Вчера";
        final var response = openAiAssistant.runTextPipeline(message, Optional.empty(), Set.of());
        final var yesterday = Instant.now().minus(1, ChronoUnit.DAYS).atZone(ZoneId.systemDefault()).toLocalDate();
        final var actualDate = response.getMentionedDate().atZone(ZoneId.systemDefault()).toLocalDate();
        assertEquals(yesterday, actualDate);
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
            final var start = Instant.now();
            final var additionalMessage = "Sending you an invoice for the last tasks";
            final var response = openAiAssistant.runFilePipeline(fileUrl.get(), additionalMessage, Optional.empty(), Set.of());
            assertEquals(expectedAmount.get(), response.getAmount());
            assertEquals(expectedCurrency.get(), response.getCurrency());
            System.out.println(response);
            final var end = Instant.now();
            System.out.println("Execution time: " + Duration.between(start, end).toMillis() + "ms");
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
            fileUrl = () -> "https://volee-avatars-dev-us.s3.amazonaws.com/ai-counting/Check.jpg";
            expectedAmount = () -> 1004850d;
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
