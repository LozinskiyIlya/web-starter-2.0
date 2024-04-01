package com.starter.web.configuration.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@Data
@Validated
@ConfigurationProperties(prefix = "starter.jwt")
public class JwtProperties {

    @NotBlank
    private String secret;
}
