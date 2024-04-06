package com.starter.common.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan("com.starter.common")
@Import({com.starter.domain.configuration.DomainAutoconfiguration.class})
public class CommonAutoconfiguration {
}
