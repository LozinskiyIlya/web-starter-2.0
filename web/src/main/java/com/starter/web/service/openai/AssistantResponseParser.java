package com.starter.web.service.openai;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starter.web.fragments.BillAssistantResponse;
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

    public BillAssistantResponse preparse(final String response) {
        try {
            return mapper.readValue(response, BillAssistantResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse response: {}", response, e);
            return BillAssistantResponse.EMPTY();
        }
    }
}
