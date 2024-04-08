package com.starter.openai.service;

import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author ilya
 * @date 20.04.2023
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiAssistant {

    private static final String MODEL = "gpt-4-1106-preview";
    private static final List<String> STOP = List.of("0.0");
    private static final double TEMPERATURE = 0.25;
    private static final int CHOICES = 1;
    private static final int MAX_TOKENS = 512;
    private static final int MAX_TEXT_LENGTH = 1024;

    private final OpenAiService openAiService;


    public String completion(String prompt) {
        CompletionRequest completionRequest = CompletionRequest.builder()
                .model(MODEL)
                .n(CHOICES)
                .stop(STOP)
                .temperature(TEMPERATURE)
                .prompt(prompt)
                .build();
        return openAiService.createCompletion(completionRequest).getChoices().get(0).getText();
    }

    public String chatCompletion(String prompt) {
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .model(MODEL)
                .n(CHOICES)
                .maxTokens(MAX_TOKENS)
                .stop(STOP)
                .temperature(TEMPERATURE)
                .messages(List.of(new ChatMessage("user", prompt)))
                .build();
        return openAiService.createChatCompletion(completionRequest).getChoices().get(0).getMessage().getContent();
    }

    private String withMaxTextLength(String text) {
        return text != null && text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) + "..." : text;
    }
}
