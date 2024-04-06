package com.starter.common.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@Data
@Validated
@ConfigurationProperties(prefix = "starter.jwt")
public class JwtProperties {

    @NotBlank
    private String secret;

    @NotNull
    private long daysValid;
}
