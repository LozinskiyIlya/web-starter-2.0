package com.starter.web.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableConfigurationProperties(ServerProperties.class)
@Import({com.starter.domain.configuration.DomainAutoconfiguration.class, com.starter.common.config.CommonAutoconfiguration.class})
public class WebAutoconfiguration {
}
