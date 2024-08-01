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

import java.time.Instant;
import java.util.LinkedList;
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
    private static final String ASSISTANT_MODEL = "gpt-4o";
    private static final String ASSISTANT_ID = "asst_Y7NTF6GZ906pAsqh9t9Aac6G";
    private static final List<String> STOP = List.of("0.0");
    private static final double TEMPERATURE = 0.25;
    private static final int CHOICES = 1;
    private static final int MAX_TOKENS = 1024;
    private static final int MAX_TEXT_LENGTH = 1024;

    private final OpenAiService openAiService;
    private final OpenAiFileManager openAiFileManager;
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

    public BillAssistantResponse runFilePipeline(UUID userId, String filePath, @Nullable String caption, @Nullable String defaultCurrency) {
        final var withedCaption = caption != null ? withMaxTextLength(caption) : "";
        final var currencyPrompt = defaultCurrency != null ? String.format(DEFAULT_CURRENCY_PROMPT, defaultCurrency) : "";
        final var filePrompt = String.format(FILE_PROMPT, currencyPrompt, withedCaption);
        final var uploaded = openAiFileManager.uploadFile(filePath);
        try {
            final var threadRun = openAiService.createThreadAndRun(CreateThreadAndRunRequest.builder()
                    .assistantId(ASSISTANT_ID)
                    .instructions(instruct())
                    .metadata(Map.of("user_id", userId.toString()))
                    .thread(ThreadRequest.builder()
                            .messages(List.of(MessageRequest.builder()
                                            .role("assistant")
                                            .fileIds(List.of(uploaded.getId()))
                                            .content(filePrompt)
                                            .build(),
                                    MessageRequest.builder()
                                            .role("user")
                                            .content(FILE_ADDITIONAL_PROMPT)
                                            .build()))
                            .build())
                    .build()
            );
            final var response = waitForPipelineCompletion(threadRun);
            return responseParser.parse(response);
        } finally {
            openAiFileManager.deleteFile(uploaded.getId());
        }
    }

    public BillAssistantResponse runTextPipeline(UUID userId, String forwardedMessage, @Nullable String defaultCurrency) {
        final var withed = withMaxTextLength(forwardedMessage);
        final var listOfMessages = new LinkedList<MessageRequest>();
        if (defaultCurrency != null) {
            listOfMessages.add(MessageRequest.builder()
                    .role("assistant")
                    .content(String.format(DEFAULT_CURRENCY_PROMPT, defaultCurrency))
                    .build());
        }
        listOfMessages.add(MessageRequest.builder()
                .role("user")
                .content(withed)
                .build());
        final var threadRun = openAiService.createThreadAndRun(CreateThreadAndRunRequest.builder()
                .assistantId(ASSISTANT_ID)
                .instructions(instruct())
                .thread(ThreadRequest.builder().messages(listOfMessages).build())
                .build()
        );
        final var response = waitForPipelineCompletion(threadRun);
        log.info("Text pipeline response: {}", response);
        return responseParser.parse(response);
    }

    public MessageClassificationResponse classifyMessage(String prompt) {
        final var response = chatCompletion(PRE_PROCESS_PROMPT, withMaxTextLength(prompt));
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

    private String withMaxTextLength(String text) {
        final var withed = text != null && text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) + "..." : text;
        return withed.trim();
    }

    private static String instruct(){
        return "Current date: " + Instant.now();
    }

    private static final String INSIGHTS_INSTRUCTIONS = """
            Analyse the following bills and give short 2 sentences suggestions 'insights'.
            Analyse trends over time, categories, amounts, and other patterns.
            Do not go into great details by mentioning exact bill entry anywhere, keep it simple and clear.
            Do not include markup, your response should look like a short paragraph. The shorter the better.
            """;

    private static final String PRE_PROCESS_PROMPT = """
            Is this a payment related message?
            Respond with nothing more than valid JSON (WITHOUT ``` marks) of the format:
            {
                "payment_related": true | false
            }
            It is most likely true if there is an amount and the purpose mentioned in the message.
            """;
    private static final String DEFAULT_CURRENCY_PROMPT = "If currency is not parseable use %s";
    private static final String FILE_PROMPT = "%s\n%s\nAnalyse the file according to your instructions";
    private static final String FILE_ADDITIONAL_PROMPT = "Yes, you DO have the file. In case of error try again. DO NOT include any comments, respond only with the resulting JSON filled according to the file's content.";
}
