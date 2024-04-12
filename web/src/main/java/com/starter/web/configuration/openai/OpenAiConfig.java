package com.starter.web.configuration.openai;

import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ilya
 * @date 08.01.2024
 */

@Configuration
@EnableConfigurationProperties({OpenAiProperties.class, AssistantProperties.class})
@RequiredArgsConstructor
public class OpenAiConfig {

    private final OpenAiProperties openAiProperties;

    @Bean
    OpenAiService openAiService() {
        return new OpenAiService(openAiProperties.getToken(), openAiProperties.getTimeout());
    }
}
