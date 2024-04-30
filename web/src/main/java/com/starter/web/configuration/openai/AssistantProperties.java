package com.starter.web.configuration.openai;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "starter.open-ai.assistant")
public class AssistantProperties {
    private final String[] billTags = {"Food", "Transport", "Entertainment", "Health", "Education", "Shopping", "Work", "Rent"};
    private final String[] tagsColors = {"#27A81E", "#16B5E8", "#FB4090", "#E4912D", "#2893FF", "#FC5E15", "#F4213C", "#6224FF"};
}
