package com.starter.common.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;


@Data
@Validated
@ConfigurationProperties(prefix = "starter.server")
public class ServerProperties {

    @NotNull
    private URI host;
}
