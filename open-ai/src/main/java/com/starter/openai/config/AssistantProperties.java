package com.starter.openai.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "starter.open-ai.assistant")
public class AssistantProperties {
    private final String[] billTags = {"Food", "Transport", "Entertainment", "Health", "Education", "Shopping", "Work", "Rent"};
}
