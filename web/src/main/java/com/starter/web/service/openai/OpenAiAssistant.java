package com.starter.web.service.openai;

import com.starter.web.fragments.BillAssistantResponse;
import com.starter.web.fragments.MessageClassificationResponse;
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
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.starter.web.service.openai.StaticPromptRenderer.*;

/**
 * @author ilya
 * @date 20.04.2023
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiAssistant {
    public static final String ASSISTANT_MODEL = "gpt-4o";
    public static final String VISION_MODEL = "gpt-4o-mini";
    public static final int MAX_TOKENS = 1024;
    private static final String ASSISTANT_ID = "asst_Y7NTF6GZ906pAsqh9t9Aac6G";
    private static final List<String> STOP = List.of("0.0");
    private static final double TEMPERATURE = 0.25;
    private static final int CHOICES = 1;

    private final OpenAiService openAiService;
    private final OpenAiFileManager openAiFileManager;
    private final ImgToTextTransformer transformer;
    private final AssistantResponseParser responseParser;

    public String chatCompletion(String system, String prompt) {
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .model(ASSISTANT_MODEL)
                .n(CHOICES)
                .stop(STOP)
                .maxTokens(MAX_TOKENS)
                .temperature(TEMPERATURE)
                .messages(List.of(
                        new ChatMessage("system", system),
                        new ChatMessage("user", prompt)
                ))
                .build();
        return openAiService.createChatCompletion(completionRequest).getChoices().get(0).getMessage().getContent();
    }

    public BillAssistantResponse runFilePipeline(UUID userId, String filePathOrBase64String, @Nullable String caption, @Nullable String defaultCurrency) {
        final var extension = filePathOrBase64String.substring(filePathOrBase64String.lastIndexOf('.') + 1);
        if (!extension.equals("pdf")) {
            final var textOnImage = transformer.visionTransform(filePathOrBase64String, caption);
            return runTextPipeline(userId, textOnImage, defaultCurrency);
        }
        final var filePrompt = fullFilePrompt(caption, defaultCurrency);
        final var uploaded = openAiFileManager.uploadFile(filePathOrBase64String);
        try {
            final var threadRun = openAiService.createThreadAndRun(CreateThreadAndRunRequest.builder()
                    .assistantId(ASSISTANT_ID)
                    .metadata(Map.of("user_id", userId.toString()))
                    .thread(ThreadRequest.builder()
                            .messages(List.of(MessageRequest.builder()
                                            .role("assistant")
                                            .fileIds(List.of(uploaded.getId()))
                                            .content(filePrompt)
                                            .build(),
                                    MessageRequest.builder()
                                            .role("user")
                                            .content(FORCE_FILE_USE_PROMPT)
                                            .build()))
                            .build())
                    .build()
            );
            final var response = waitForPipelineCompletion(threadRun);
            return responseParser.parse(response);
        } finally {
            openAiFileManager.deleteRemoteFile(uploaded.getId());
        }
    }

    public BillAssistantResponse runTextPipeline(UUID userId, String forwardedMessage, @Nullable String defaultCurrency) {
        final var withed = trimUserMessage(forwardedMessage);
        final var listOfMessages = new LinkedList<MessageRequest>();
        listOfMessages.add(MessageRequest.builder()
                .role("assistant")
                .content(runInstructions(defaultCurrency))
                .build());
        listOfMessages.add(MessageRequest.builder()
                .role("user")
                .content(withed)
                .build());
        final var threadRun = openAiService.createThreadAndRun(CreateThreadAndRunRequest.builder()
                .assistantId(ASSISTANT_ID)
                .thread(ThreadRequest.builder().messages(listOfMessages).build())
                .build()
        );
        final var response = waitForPipelineCompletion(threadRun);
        log.info("Text pipeline response: {}", response);
        return responseParser.parse(response);
    }

    public MessageClassificationResponse classifyMessage(String prompt) {
        final var response = chatCompletion(PRE_PROCESS_PROMPT, trimUserMessage(prompt));
        log.info("Text classification response: {}", response);
        return responseParser.parseClassification(response);
    }

    public String getInsights(String prompt) {
        final var response = chatCompletion(INSIGHTS_INSTRUCTIONS, prompt);
        log.info("Insights response: {}", response);
        return response;
    }

    private String waitForPipelineCompletion(Run run) {
        while (!"completed".equals(run.getStatus())) {
            run = openAiService.retrieveRun(run.getThreadId(), run.getId());
        }
        return openAiService.listMessages(run.getThreadId())
                .getData()
                .stream()
                .peek(m -> log.info("Message: {}", m))
                .findFirst()
                .map(Message::getContent)
                .map(l -> l.get(l.size() - 1))
                .map(MessageContent::getText)
                .map(Text::getValue).orElse("");
    }
}
