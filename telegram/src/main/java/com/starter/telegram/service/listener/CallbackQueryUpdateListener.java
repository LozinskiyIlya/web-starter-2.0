package com.starter.telegram.service.listener;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Update;
import com.starter.domain.entity.Bill.BillStatus;
import com.starter.domain.repository.BillRepository;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackQueryUpdateListener implements UpdateListener {
    public static final String CONFIRM_BILL_PREFIX = "confirm_bill_";
    private final BillRepository billRepository;
    private final TelegramMessageRenderer renderer;

    @Override
    public void processUpdate(Update update, TelegramBot bot) {
        final var callbackQuery = update.callbackQuery();
        final var chatId = callbackQuery.from().id();
        if (callbackQuery.data().startsWith(CONFIRM_BILL_PREFIX)) {
            confirmBill(bot, callbackQuery, chatId);
        }
    }

    private void confirmBill(TelegramBot bot, CallbackQuery callbackQuery, Long chatId) {
        final var message = callbackQuery.maybeInaccessibleMessage();
        final var billId = UUID.fromString(callbackQuery.data().substring(CONFIRM_BILL_PREFIX.length()));
        final var bill = billRepository.findById(billId).orElseThrow();
        bill.setStatus(BillStatus.CONFIRMED);
        billRepository.save(bill);
        log.info("Bill confirmed: {}", bill);
        final var messageUpdate = renderer.renderBillUpdate(chatId, bill, message);
        bot.execute(messageUpdate);
    }
}
