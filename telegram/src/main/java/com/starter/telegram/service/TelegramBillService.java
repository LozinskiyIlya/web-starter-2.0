package com.starter.telegram.service;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import com.starter.common.events.BillConfirmedEvent;
import com.starter.common.events.BillCreatedEvent;
import com.starter.common.events.NotPaymentRelatedEvent;
import com.starter.common.events.ProcessingErrorEvent;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Bill.BillStatus;
import com.starter.domain.entity.User;
import com.starter.domain.entity.UserSettings;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.UserSettingsRepository;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static com.starter.telegram.service.TelegramBotService.latestKeyboard;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBillService {

    public static final String NOT_RECOGNIZED_MESSAGE = "The message does not appear to be payment-related. Please edit the message or send another one";
    public static final String PROCESSING_ERROR_MESSAGE = "We're experiencing too many requests to our recognition services at the moment. Please try sending your message again.";
    private final TelegramBot bot;
    private final TelegramMessageRenderer renderer;
    private final BillRepository billRepository;
    private final UserSettingsRepository userSettingsRepository;

    @Async
    @Transactional
    @EventListener
    public void onBillCreated(BillCreatedEvent event) {
        final var billId = event.getPayload();
        log.info("Sending bill to confirmation: {}", billId);
        // send to tg
        final var bill = billRepository.findById(billId).orElseThrow();
        final var group = bill.getGroup();
        final var ownerInfo = group.getOwner().getUserInfo();
        final var userSettings = userSettingsRepository.findOneByUser(group.getOwner());
        final var autoConfirm = userSettings.map(UserSettings::getAutoConfirmBills).orElse(false);

        if (autoConfirm) {
            log.info("Auto-confirm enabled, sending only confirmed message");
            bill.setMessageId(Bill.DEFAULT_MESSAGE_ID);
            billRepository.save(bill);
            onBillConfirmed(new BillConfirmedEvent(this, billId));
            return;
        }

        final var spoilerBills = userSettings.map(UserSettings::getSpoilerBills).orElse(true);
        final var billMessage = renderer.renderBill(ownerInfo.getTelegramChatId(), bill, spoilerBills);
        final var message = bot.execute(billMessage);
        // change status, message id and save
        bill.setMessageId(message.message().messageId());
        bill.setStatus(BillStatus.SENT);
        billRepository.save(bill);
        log.info("Bill sent, message id: {}", message.message().messageId());
    }

    @Async
    @Transactional
    @EventListener
    public void onBillConfirmed(BillConfirmedEvent event) {
        final var billId = event.getPayload();
        // send to tg
        final var bill = billRepository.findById(billId).orElseThrow();
        if (bill.getStatus().equals(BillStatus.CONFIRMED)) {
            // do nothing if bill was priorly confirmed
            return;
        }

        bill.setStatus(BillStatus.CONFIRMED);
        billRepository.save(bill);
        log.info("Bill {} confirmed, updating status and message...", bill.getId());

        final var group = bill.getGroup();
        final var ownerInfo = group.getOwner().getUserInfo();
        // update message in owner chat
        final var tgMessage = new SelfMadeTelegramMessage();
        tgMessage.setMessageId(bill.getMessageId());
        final var message = renderer.renderBillConfirmation(ownerInfo.getTelegramChatId(), bill, tgMessage);
        bot.execute(message);

        // send to all members
        log.info("Status and message updated. Sending bill to {} members", group.getMembers().size());
        group.getMembers()
                .stream()
                .map(User::getUserInfo)
                .filter(userInfo -> userInfo.getId() != ownerInfo.getId())
                .map(userInfo -> {
                    final var chatId = userInfo.getTelegramChatId();
                    final var spoilerBills = userSettingsRepository.findOneByUser(userInfo.getUser())
                            .map(UserSettings::getSpoilerBills)
                            .orElse(true);
                    return renderer.renderBillPreview(chatId, bill, spoilerBills);
                })
                .forEach(bot::execute);
        log.info("Bill sent to all members");
    }

    @Async
    @EventListener
    public void onNotRecognizedBill(NotPaymentRelatedEvent event) {
        final var chatId = event.getPayload();
        final var message = new SendMessage(chatId, NOT_RECOGNIZED_MESSAGE);
        bot.execute(message.replyMarkup(latestKeyboard()));
    }

    @Async
    @EventListener
    public void onProcessingError(ProcessingErrorEvent event) {
        final var chatId = event.getPayload();
        final var message = new SendMessage(chatId, PROCESSING_ERROR_MESSAGE);
        bot.execute(message.replyMarkup(latestKeyboard()));
    }

    @Deprecated
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SelfMadeTelegramMessage extends Message {
        public void setMessageId(int messageId) {
            super.message_id = messageId;
        }
    }
}
