package com.starter.openai.service;

import com.theokanning.openai.file.File;
import com.theokanning.openai.service.OpenAiService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ResourceLoader;


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
        Mockito.when(openAiService.uploadFile(Mockito.anyString(), Mockito.anyString())).thenReturn(new File());
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