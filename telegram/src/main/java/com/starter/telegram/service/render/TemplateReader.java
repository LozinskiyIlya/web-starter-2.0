package com.starter.telegram.service.render;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class TemplateReader {

    private final static String DEFAULT_PREFIX = "templates/";

    @SneakyThrows
    public String read(String templateFile) {
        var template = new ClassPathResource(DEFAULT_PREFIX + templateFile);
        try (InputStream html = template.getInputStream()) {
            ByteSource byteSource = new ByteSource() {
                @Override
                public InputStream openStream() {
                    return html;
                }
            };
            return byteSource.asCharSource(Charsets.UTF_8).read();
        }
    }
}

