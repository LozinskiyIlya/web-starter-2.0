package com.starter.telegram.service;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.starter.common.events.BillConfirmedEvent;
import com.starter.common.events.BillCreatedEvent;
import com.starter.domain.entity.Bill.BillStatus;
import com.starter.domain.entity.User;
import com.starter.domain.entity.UserInfo;
import com.starter.domain.repository.BillRepository;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.function.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBillService {

    private final TelegramBot bot;
    private final TelegramMessageRenderer renderer;
    private final BillRepository billRepository;

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
        final var billMessage = renderer.renderBill(ownerInfo.getTelegramChatId(), bill);
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
        log.info("Bill {} confirmed, updating status and message...", billId);
        // send to tg
        final var bill = billRepository.findById(billId).orElseThrow();
        bill.setStatus(BillStatus.CONFIRMED);
        billRepository.save(bill);
        final var group = bill.getGroup();
        final var ownerInfo = group.getOwner().getUserInfo();
        if (bill.getMessageId() == null) {
            return;
        }
        // update message in owner chat
        final var tgMessage = new SelfMadeMessage();
        tgMessage.setMessageId(bill.getMessageId());
        final var message = renderer.renderBillUpdate(ownerInfo.getTelegramChatId(), bill, tgMessage);
        bot.execute(message);
        // send to all members
        log.info("Status and message updated. Sending bill to all members");
        group.getMembers()
                .stream()
                .map(User::getUserInfo)
                .map(UserInfo::getTelegramChatId)
                .filter(Predicate.not(ownerInfo.getTelegramChatId()::equals))
                .map(chatId -> renderer.renderBillPreview(chatId, bill))
                .forEach(bot::execute);
        log.info("Bill sent to all members");
    }


    @Deprecated
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SelfMadeMessage extends Message {
        public void setMessageId(int messageId) {
            super.message_id = messageId;
        }
    }
}
