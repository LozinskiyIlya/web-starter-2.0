package com.starter.telegram.listener.query;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.starter.common.events.BillConfirmedEvent;
import com.starter.domain.entity.Bill;
import com.starter.domain.repository.BillRepository;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillCallbackExecutor implements CallbackExecutor {

    private static final String CALLBACK_PREFIX = "bill_";
    public static final String CONFIRM_BILL_PREFIX = CALLBACK_PREFIX + "confirm_";
    public static final String SKIP_BILL_PREFIX = CALLBACK_PREFIX + "skip_";

    private final BillRepository billRepository;
    private final ApplicationEventPublisher publisher;
    private final TelegramMessageRenderer renderer;

    @Override
    public void execute(TelegramBot bot, CallbackQuery query, Long chatId) {
        final var callbackData = query.data();
        if (callbackData.startsWith(CONFIRM_BILL_PREFIX)) {
            confirmBill(query);
        } else if (callbackData.startsWith(SKIP_BILL_PREFIX)) {
            skipBill(bot, query, chatId);
        }
    }

    private void confirmBill(CallbackQuery query) {
        final var billId = extractId(query, CONFIRM_BILL_PREFIX);
        publisher.publishEvent(new BillConfirmedEvent(this, billId));
    }

    private void skipBill(TelegramBot bot, CallbackQuery query, Long chatId) {
        final var message = query.maybeInaccessibleMessage();
        final var billId = extractId(query, SKIP_BILL_PREFIX);
        final var bill = billRepository.findById(billId).orElseThrow();
        bill.setStatus(Bill.BillStatus.SKIPPED);
        billRepository.save(bill);
        log.info("Bill skipped: {}", bill);
        final var messageUpdate = renderer.renderBillSkipped(chatId, bill, message);
        bot.execute(messageUpdate);
    }

    @Override
    public String getPrefix() {
        return CALLBACK_PREFIX;
    }
}
