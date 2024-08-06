package com.starter.web.service;


import com.starter.common.events.*;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Group;
import com.starter.domain.entity.UserSettings;
import com.starter.domain.repository.GroupRepository;
import com.starter.domain.repository.UserSettingsRepository;
import com.starter.web.fragments.BillAssistantResponse;
import com.starter.web.service.bill.BillService;
import com.starter.web.service.openai.OpenAiAssistant;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;

import static com.starter.common.utils.CustomFileUtils.deleteLocalFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessor {
    private static final int MIN_FIELDS_FILLED = 3;
    private static final double MIN_VALID_AMOUNT = 0.01;

    private final ApplicationEventPublisher publisher;
    private final OpenAiAssistant openAiAssistant;
    private final BillService billService;
    private final GroupRepository groupRepository;
    private final UserSettingsRepository userSettingsRepository;

    @Async
    @Transactional
    @EventListener
    void processMessage(TelegramTextMessageEvent event) {
        final var payload = event.getPayload();
        final var groupId = payload.getFirst();
        final var content = payload.getSecond();
        final var group = groupRepository.findById(groupId).orElseThrow();
        try {
            final var response = openAiAssistant.runTextPipeline(group.getOwner().getId(), content, group.getDefaultCurrency());
            save(group, response, Bill.DEFAULT_MESSAGE_ID);
        } catch (Exception e) {
            log.error("Error while processing message", e);
            publisher.publishEvent(new ProcessingErrorEvent(this, Pair.of(group.getChatId(), Bill.DEFAULT_MESSAGE_ID)));
        }
    }

    @Async
    @Transactional
    @EventListener
    void processMessage(TelegramFileMessageEvent event) {
        final var payload = event.getPayload();
        final var groupId = payload.groupId();
        final var caption = payload.caption();
        final var fileUrl = payload.fileUrl();
        final var messageId = payload.messageId();
        final var group = groupRepository.findById(groupId).orElseThrow();
        try {
            final var response = openAiAssistant.runFilePipeline(group.getOwner().getId(), fileUrl, caption, group.getDefaultCurrency());
            save(group, response, messageId);
        } catch (Exception e) {
            log.error("Error while processing message", e);
            publisher.publishEvent(new ProcessingErrorEvent(this, Pair.of(group.getChatId(), messageId)));
        } finally {
            deleteLocalFile(fileUrl);
        }
    }

    public boolean shouldSave(Group group, BillAssistantResponse response) {
        // at least 0.01 amount should be present if setting is ON
        final var user = group.getOwner();
        final var skippingZeros = userSettingsRepository.findOneByUser(user)
                .map(UserSettings::getSkipZeros)
                .filter(skipSetting -> skipSetting && (response.getAmount() == null || response.getAmount() < MIN_VALID_AMOUNT))
                .orElse(false);

        if (skippingZeros) {
            return false;
        }

        // at least MIN_FIELDS_FILLED any fields should be filled
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

    private void save(Group group, BillAssistantResponse response, int messageId) {
        if (shouldSave(group, response)) {
            final var bill = billService.addBill(group, response);
            log.info("Bill created: {}", bill);
            publisher.publishEvent(new BillCreatedEvent(this, Pair.of(bill.getId(), messageId)));
        } else {
            log.info("Message is not payment related, skipping processing");
            publisher.publishEvent(new NotPaymentRelatedEvent(this, Pair.of(group.getChatId(), messageId)));
        }
    }
}
