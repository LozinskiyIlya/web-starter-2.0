package com.starter.web.service;


import com.starter.common.events.BillCreatedEvent;
import com.starter.common.events.NotPaymentRelatedEvent;
import com.starter.common.events.TelegramFileMessageEvent;
import com.starter.common.events.TelegramTextMessageEvent;
import com.starter.domain.entity.Group;
import com.starter.domain.repository.GroupRepository;
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

import static com.starter.common.utils.CustomFileUtils.deleteLocalFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessor {
    private static final int MIN_FIELDS_FILLED = 3;

    private final ApplicationEventPublisher publisher;
    private final OpenAiAssistant openAiAssistant;
    private final BillService billService;
    private final GroupRepository groupRepository;

    @Async
    @Transactional
    @EventListener
    public void processMessage(TelegramTextMessageEvent event) {
        final var payload = event.getPayload();
        final var groupId = payload.getFirst();
        final var message = payload.getSecond();
        final var group = groupRepository.findById(groupId).orElseThrow();
        final var isPayment = openAiAssistant.classifyMessage(message).isPaymentRelated();
        if (isPayment) {
            final var response = openAiAssistant.runTextPipeline(group.getOwner().getId(), message, group.getDefaultCurrency());
            save(group, response);
        } else {
            notRecognized(group);
        }
    }

    @Async
    @Transactional
    @EventListener
    public void processMessage(TelegramFileMessageEvent event) {
        final var payload = event.getPayload();
        final var groupId = payload.groupId();
        final var caption = payload.caption();
        final var fileUrl = payload.fileUrl();
        final var group = groupRepository.findById(groupId).orElseThrow();
        final var response = openAiAssistant.runFilePipeline(group.getOwner().getId(), fileUrl, caption, group.getDefaultCurrency());
        save(group, response);
        deleteLocalFile(fileUrl);
    }

    private boolean shouldSave(BillAssistantResponse response) {
        // at least MIN_FIELDS_FILLED any fields should be filled
        if (response.getAmount() == null || response.getAmount() < 0.01d) {
            return false;
        }
        return Arrays.stream(response.getClass().getDeclaredFields())
                .peek(f -> f.setAccessible(true))
                .filter(f -> {
                    try {
                        if (f.getType().equals(String.class)) {
                            return f.get(response) != null && !f.get(response).toString().isBlank();
                        }
                        if (f.getType().equals(Double.class)) {
                            return f.get(response) != null && (Double) f.get(response) > 0;
                        }
                        if (f.getType().equals(String[].class)) {
                            return f.get(response) != null && ((String[]) f.get(response)).length > 0;
                        }
                        return f.get(response) != null;
                    } catch (IllegalAccessException e) {
                        log.error("Error while checking if field is filled", e);
                        return false;
                    }
                }).count() >= MIN_FIELDS_FILLED;
    }

    private void save(Group group, BillAssistantResponse response) {
        if (shouldSave(response)) {
            final var bill = billService.addBill(group, response);
            log.info("Bill created: {}", bill);
            publisher.publishEvent(new BillCreatedEvent(this, bill.getId()));
        } else {
            notRecognized(group);
        }
    }

    private void notRecognized(Group group) {
        log.info("Message is not payment related, skipping processing");
        publisher.publishEvent(new NotPaymentRelatedEvent(this, group.getChatId()));
    }
}
