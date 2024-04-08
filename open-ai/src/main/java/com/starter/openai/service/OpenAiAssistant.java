package com.starter.openai.service;

import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.messages.Message;
import com.theokanning.openai.messages.MessageContent;
import com.theokanning.openai.messages.MessageRequest;
import com.theokanning.openai.messages.content.Text;
import com.theokanning.openai.runs.CreateThreadAndRunRequest;
import com.theokanning.openai.runs.Run;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.threads.ThreadRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author ilya
 * @date 20.04.2023
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiAssistant {

    private static final String MODEL = "gpt-4-1106-preview";
    private static final String ASSISTANT_ID = "asst_Y7NTF6GZ906pAsqh9t9Aac6G";
    private static final List<String> STOP = List.of("0.0");
    private static final double TEMPERATURE = 0.25;
    private static final int CHOICES = 1;
    private static final int MAX_TOKENS = 512;
    private static final int MAX_TEXT_LENGTH = 1024;

    private final OpenAiService openAiService;
    private final OpenAiFileManager openAiFileManager;

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

    public String runFilePipeline(String filePath, UUID userId) {
        final var uploaded = openAiFileManager.uploadFile(filePath);
        final var threadRun = openAiService.createThreadAndRun(CreateThreadAndRunRequest.builder()
                .assistantId(ASSISTANT_ID)
                .metadata(Map.of("user_id", userId.toString()))
                .thread(ThreadRequest.builder()
                        .messages(List.of(MessageRequest.builder()
                                        .role("user")
                                        .fileIds(List.of(uploaded.getId()))
                                        .content("Analyse the file according to your instructions")
                                        .build(),
                                MessageRequest.builder()
                                        .role("user")
                                        .content("Yes, you DO have the file. Try again")
                                        .build()))
                        .build())
                .build()
        );
        return waitForMessage(threadRun);
    }

    public String runTextPipeline(String forwardedMessage, UUID userId) {
        final var threadRun = openAiService.createThreadAndRun(CreateThreadAndRunRequest.builder()
                .assistantId(ASSISTANT_ID)
                .metadata(Map.of("user_id", userId.toString()))
                .thread(ThreadRequest.builder()
                        .messages(List.of(MessageRequest.builder()
                                        .role("user")
                                        .content(forwardedMessage)
                                        .build(),
                                MessageRequest.builder()
                                        .role("user")
                                        .content("Analise the forwarded message with your instructions")
                                        .build()
                        ))
                        .build())
                .build()
        );
        return waitForMessage(threadRun);
    }

    private String waitForMessage(Run run) {
        while (!"completed".equals(run.getStatus())) {
            run = openAiService.retrieveRun(run.getThreadId(), run.getId());
        }
        final var messages = openAiService.listMessages(run.getThreadId())
                .getData()
                .stream()
                .map(Message::getContent)
                .flatMap(List::stream)
                .map(MessageContent::getText)
                .map(Text::getValue)
                .collect(Collectors.joining("\n"));
        return run.getMetadata().get("user_id") + " " + messages;
    }

    private String withMaxTextLength(String text) {
        return text != null && text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) + "..." : text;
    }
}
