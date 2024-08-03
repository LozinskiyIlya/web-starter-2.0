package com.starter.web.service;

import com.starter.web.service.openai.OpenAiFileManager;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.file.File;
import com.theokanning.openai.service.OpenAiService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ResourceLoader;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@SpringBootTest
class OpenAiFileManagerTest {

    @Autowired
    private OpenAiFileManager openAiFileManager;

    @Autowired
    private ResourceLoader resourceLoader;

    @MockBean
    private OpenAiService openAiService;

    @BeforeEach
    void setupMocks() {
        final var chatResponseMock = mock(ChatCompletionResult.class);
        final var chatChoiceMock = mock(ChatCompletionChoice.class);
        final var chatMessageMock = mock(ChatMessage.class);
        when(chatMessageMock.getContent()).thenReturn("ai response");
        when(chatChoiceMock.getMessage()).thenReturn(chatMessageMock);
        when(chatResponseMock.getChoices()).thenReturn(List.of(chatChoiceMock));
        when(openAiService.uploadFile(Mockito.anyString(), Mockito.anyString())).thenReturn(new File());
        when(openAiService.createChatCompletion(Mockito.any()))
                .thenReturn(chatResponseMock);
    }

    @Nested
    @DisplayName("Upload file")
    class Upload {

        abstract class OnSomeFileExtension {

            protected abstract String getExtension();

            protected boolean fileShouldExist() {
                return false;
            }

            @SneakyThrows
            @Test
            @DisplayName("Deletes local PDF file after upload")
            void deletesAfterUpload() {
                final var name = String.format("files/%s/Invoice2", getExtension());
                var resource = resourceLoader.getResource("classpath:" + name + "." + getExtension());
                openAiFileManager.uploadFile(resource.getFile().getAbsolutePath());
                // assert no local pdf file:
                resource = resourceLoader.getResource("classpath:" + name + ".pdf");
                Assertions.assertEquals(fileShouldExist(), resource.exists());
            }
        }

        @Nested
        @DisplayName("On pdf file extension")
        class OnPDFFileExtension extends OnSomeFileExtension {

            @Override
            protected String getExtension() {
                return "pdf";
            }

            @Override
            protected boolean fileShouldExist() {
                return true;
            }
        }

        @Nested
        @DisplayName("On png file extension")
        class OnPNGFileExtension extends OnSomeFileExtension {

            @Override
            protected String getExtension() {
                return "png";
            }
        }
    }
}