package com.starter.common.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@Data
@ConfigurationProperties("starter.aws.cdn")
public class CdnProperties {

    @NotNull
    private URI host;
}
