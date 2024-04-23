package com.starter.telegram.configuration;

import com.starter.domain.configuration.DomainAutoconfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({DomainAutoconfiguration.class})
public class TelegramAutoconfiguration {
}
