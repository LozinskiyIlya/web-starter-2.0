package com.starter.web.configuration.openai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

/**
 * @author ilya
 * @date 08.01.2024
 */

@Data
@Validated
@ConfigurationProperties(prefix = "starter.ollama")
public class OllamaProperties {
    private final URI baseUrl = URI.create("http://localhost:11434");
}
