package com.starter.telegram.service;


import com.pengrad.telegrambot.TelegramBot;
import com.starter.common.events.BillCreatedEvent;
import com.starter.domain.entity.Bill.BillStatus;
import com.starter.domain.repository.BillRepository;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
        final var ownerInfo = bill.getGroup().getOwner().getUserInfo();
        final var billMessage = renderer.renderBill(ownerInfo.getTelegramChatId(), bill);
        bot.execute(billMessage);
        // change status and save
        bill.setStatus(BillStatus.SENT);
        billRepository.save(bill);
    }
}
