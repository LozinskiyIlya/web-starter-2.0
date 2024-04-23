package com.starter.web.service;

import com.starter.web.service.openai.AssistantResponseParser;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;
import org.testcontainers.shaded.com.google.common.base.Charsets;
import org.testcontainers.shaded.com.google.common.io.ByteSource;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class AssistantResponseParserTest {

    @Autowired
    private AssistantResponseParser responseParser;

    @Test
    @DisplayName("Parses full response")
    void parsesFullResponse() {
        var response = readResource("responses/open-ai/with_all_fields.txt");
        var parsed = responseParser.parse(response);
        assertTrue(StringUtils.hasText(parsed.getBuyer()));
        assertTrue(StringUtils.hasText(parsed.getSeller()));
        assertTrue(parsed.getAmount() > 0);
        assertTrue(StringUtils.hasText(parsed.getPurpose()));
        assertTrue(StringUtils.hasText(parsed.getCurrency()));
        assertTrue(parsed.getTags().length > 0);
        // 2023-09-01
        assertEquals("2023-09-01T00:00:00Z", parsed.getMentionedDate().toString());
    }

    @Test
    @DisplayName("Parses some fields response")
    void parsesSomeFieldsResponse() {
        var response = readResource("responses/open-ai/without_some_fields.txt");
        var parsed = responseParser.parse(response);
        assertTrue(parsed.getAmount() > 0);
        assertTrue(StringUtils.hasText(parsed.getPurpose()));
        assertTrue(StringUtils.hasText(parsed.getCurrency()));
        assertTrue(parsed.getTags().length > 0);
        // Nov 2023
        assertEquals("2023-11-01T00:00:00Z", parsed.getMentionedDate().toString());
    }

    @Test
    @DisplayName("Parses empty response")
    void parsesEmptyResponse() {
        var response = readResource("responses/open-ai/without_all_fields.txt");
        var parsed = responseParser.parse(response);
        assertNotNull(parsed);
        assertNotNull(parsed.getMentionedDate());
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