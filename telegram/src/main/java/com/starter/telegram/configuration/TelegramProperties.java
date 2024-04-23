package com.starter.telegram.configuration;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "starter.telegram")
public class TelegramProperties {

    private final String token;
}
