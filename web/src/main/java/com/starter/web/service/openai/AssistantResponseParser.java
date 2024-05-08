package com.starter.web.service.openai;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starter.web.fragments.BillAssistantResponse;
import com.starter.web.fragments.MessageClassificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantResponseParser {

    private final ObjectMapper mapper;

    public BillAssistantResponse parse(final String response) {
        final var preparsed = preparse(response);
        if (preparsed.getMentionedDate() == null) {
            preparsed.setMentionedDate(Instant.now());
        }
        // todo add more parsing logic when target fields are empty
        return preparsed;
    }

    private BillAssistantResponse preparse(final String response) {
        try {
            return mapper.readValue(getRidOfOpenAiJsonFormatting(response), BillAssistantResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse response: {}", response, e);
            return BillAssistantResponse.EMPTY();
        }
    }

    public MessageClassificationResponse parseClassification(final String response) {
        try {
            return mapper.readValue(getRidOfOpenAiJsonFormatting(response), MessageClassificationResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse classification response: {}", response, e);
            return new MessageClassificationResponse();
        }
    }

    private String getRidOfOpenAiJsonFormatting(String response) {
        if (response.startsWith("```json")) {
            return response.substring(7, response.length() - 3);
        } else {
            return response;
        }
    }
}
