package com.starter.web.service;


import com.starter.common.events.TelegramTextMessageEvent;
import com.starter.domain.entity.Group;
import com.starter.web.fragments.BillAssistantResponse;
import com.starter.web.service.bill.BillService;
import com.starter.web.service.openai.OpenAiAssistant;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessor {
    private static final int MIN_FIELDS_FILLED = 3;
    private final OpenAiAssistant openAiAssistant;
    private final BillService billService;


    @Transactional
    @EventListener
    public void processMessage(TelegramTextMessageEvent event) {
        final var payload = event.getPayload();
        final var group = payload.getFirst();
        final var message = payload.getSecond();
        final var isPayment = openAiAssistant.classifyMessage(message).isPaymentRelated();
        if (isPayment) {
            final var response = openAiAssistant.runTextPipeline(message, group.getOwner().getId());
            if (shouldSave(response)) {
                billService.addBill(group, response);
            }
        }
    }

    public boolean shouldSave(BillAssistantResponse response) {
        // at least MIN_FIELDS_FILLED any fields should be filled
        return Arrays.stream(response.getClass().getDeclaredFields())
                .peek(f -> f.setAccessible(true))
                .filter(f -> {
                    try {
                        return f.get(response) != null;
                    } catch (IllegalAccessException e) {
                        log.error("Error while checking if field is filled", e);
                        return false;
                    }
                }).count() >= MIN_FIELDS_FILLED;
    }
}
