package com.starter.openai.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starter.openai.fragments.BillAssistantResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantResponseParser {

    private static final ObjectMapper mapper = new ObjectMapper();

    public BillAssistantResponse parse(String response) {
        try {
            return mapper.readValue(response, BillAssistantResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse response: {}", response, e);
            return BillAssistantResponse.EMPTY();
        }
    }
}
