package com.starter.web.service.openai;

import com.starter.web.fragments.BillAssistantResponse;
import com.starter.web.fragments.MessageClassificationResponse;
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
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author ilya
 * @date 20.04.2023
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiAssistant {

    private static final String ASSISTANT_MODEL = "gpt-4-1106-preview";
    private static final String MESSAGE_CLASSIFIER_MODEL = "gpt-3.5-turbo-0125";
    private static final String ASSISTANT_ID = "asst_Y7NTF6GZ906pAsqh9t9Aac6G";
    private static final List<String> STOP = List.of("0.0");
    private static final double TEMPERATURE = 0.25;
    private static final int CHOICES = 1;
    private static final int MAX_TOKENS = 512;
    private static final int MAX_TEXT_LENGTH = 1024;

    private final OpenAiService openAiService;
    private final OpenAiFileManager openAiFileManager;
    private final AssistantResponseParser responseParser;

    public String completion(String prompt) {
        CompletionRequest completionRequest = CompletionRequest.builder()
                .model(ASSISTANT_MODEL)
                .n(CHOICES)
                .stop(STOP)
                .temperature(TEMPERATURE)
                .prompt(prompt)
                .build();
        return openAiService.createCompletion(completionRequest).getChoices().get(0).getText();
    }

    public BillAssistantResponse runFilePipeline(UUID userId, String filePath, @Nullable String caption) {
        final var withedCaption = caption != null ? withMaxTextLength(caption) : "";
        final var uploaded = openAiFileManager.uploadFile(filePath);
        final var threadRun = openAiService.createThreadAndRun(CreateThreadAndRunRequest.builder()
                .assistantId(ASSISTANT_ID)
                .metadata(Map.of("user_id", userId.toString()))
                .thread(ThreadRequest.builder()
                        .messages(List.of(MessageRequest.builder()
                                        .role("user")
                                        .fileIds(List.of(uploaded.getId()))
                                        .content(withedCaption + "\nAnalyse the file according to your instructions")
                                        .build(),
                                MessageRequest.builder()
                                        .role("user")
                                        .content("Respond with nothing more than a valid JSON")
                                        .build(),
                                MessageRequest.builder()
                                        .role("user")
                                        .content("Yes, you DO have the file. Try again")
                                        .build()))
                        .build())
                .build()
        );
        final var response = waitForPipelineCompletion(threadRun);
        openAiFileManager.deleteFile(uploaded.getId());
        return responseParser.parse(response);
    }

    public BillAssistantResponse runTextPipeline(UUID userId, String forwardedMessage) {
        final var withed = withMaxTextLength(forwardedMessage);
        final var threadRun = openAiService.createThreadAndRun(CreateThreadAndRunRequest.builder()
                .assistantId(ASSISTANT_ID)
                .metadata(Map.of("user_id", userId.toString()))
                .thread(ThreadRequest.builder()
                        .messages(List.of(MessageRequest.builder()
                                        .role("user")
                                        .content(withed)
                                        .build(),
                                MessageRequest.builder()
                                        .role("user")
                                        .content("Analise the forwarded message with your instructions")
                                        .build()
                        ))
                        .build())
                .build()
        );
        final var response = waitForPipelineCompletion(threadRun);
        log.info("Text pipeline response: {}", response);
        return responseParser.parse(response);
    }

    public MessageClassificationResponse classifyMessage(String prompt) {
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .model(MESSAGE_CLASSIFIER_MODEL)
                .n(CHOICES)
                .maxTokens(MAX_TOKENS)
                .stop(STOP)
                .temperature(TEMPERATURE)
                .messages(List.of(
                        new ChatMessage("system", PRE_PROCESS_PROMPT),
                        new ChatMessage("user", prompt)
                ))
                .build();
        final var response = openAiService.createChatCompletion(completionRequest).getChoices().get(0).getMessage().getContent();
        log.info("Text classification response: {}", response);
        return responseParser.parseClassification(response);
    }

    private String waitForPipelineCompletion(Run run) {
        while (!"completed".equals(run.getStatus())) {
            run = openAiService.retrieveRun(run.getThreadId(), run.getId());
        }
        return openAiService.listMessages(run.getThreadId())
                .getData()
                .stream()
                .findFirst()
                .map(Message::getContent)
                .map(l -> l.get(l.size() - 1))
                .map(MessageContent::getText)
                .map(Text::getValue).orElse("");
    }

    private String withMaxTextLength(String text) {
        final var withed = text != null && text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) + "..." : text;
        return withed.trim();
    }

    private static final String PRE_PROCESS_PROMPT = """
            Is this a payment related message?
            Respond with nothing more than valid JSON of the format:
            {
                "payment_related": true | false
            }
            """;
}
