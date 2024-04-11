package com.starter.openai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * @author ilya
 * @date 08.01.2024
 */

@Data
@Validated
@ConfigurationProperties(prefix = "starter.open-ai")
public class OpenAiProperties {
    private final String token;
    private final Duration timeout = Duration.of(60, SECONDS);
}
