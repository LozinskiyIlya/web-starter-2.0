package com.starter.web.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({com.starter.domain.configuration.DomainAutoconfiguration.class})
public class WebAutoconfiguration {
}
