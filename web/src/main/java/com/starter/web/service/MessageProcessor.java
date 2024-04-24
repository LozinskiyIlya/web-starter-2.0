package com.starter.web.service;


import com.starter.common.events.BillCreatedEvent;
import com.starter.common.events.TelegramFileMessageEvent;
import com.starter.common.events.TelegramTextMessageEvent;
import com.starter.common.utils.CustomFileUtils;
import com.starter.web.fragments.BillAssistantResponse;
import com.starter.web.service.bill.BillService;
import com.starter.web.service.openai.OpenAiAssistant;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessor {
    private static final int MIN_FIELDS_FILLED = 3;

    private final ApplicationEventPublisher publisher;
    private final OpenAiAssistant openAiAssistant;
    private final BillService billService;

    @Async
    @Transactional
    @EventListener
    public void processMessage(TelegramTextMessageEvent event) {
        final var payload = event.getPayload();
        final var group = payload.getFirst();
        final var message = payload.getSecond();
        final var isPayment = openAiAssistant.classifyMessage(message).isPaymentRelated();
        if (isPayment) {
            final var response = openAiAssistant.runTextPipeline(group.getOwner().getId(), message);
            if (shouldSave(response)) {
                final var bill = billService.addBill(group, response);
                log.info("Bill created: {}", bill);
                publisher.publishEvent(new BillCreatedEvent(this, bill));
            }
        }
    }

    @Async
    @Transactional
    @EventListener
    public void processMessage(TelegramFileMessageEvent event) {
        final var payload = event.getPayload();
        final var group = payload.group();
        final var caption = payload.caption();
        final var fileUrl = payload.fileUrl();
        final var response = openAiAssistant.runFilePipeline(group.getOwner().getId(), fileUrl, caption);
        if (shouldSave(response)) {
            final var bill = billService.addBill(group, response);
            log.info("Bill created: {}", bill);
            publisher.publishEvent(new BillCreatedEvent(this, bill));
        }
        CustomFileUtils.deleteLocalFile(fileUrl);
    }

    private boolean shouldSave(BillAssistantResponse response) {
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
