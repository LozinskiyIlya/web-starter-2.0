package com.starter.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan("com.starter.common")
@Import({com.starter.domain.configuration.DomainAutoconfiguration.class})
@EnableConfigurationProperties({ServerProperties.class, BetaFeaturesProperties.class})
public class CommonAutoconfiguration {
}
