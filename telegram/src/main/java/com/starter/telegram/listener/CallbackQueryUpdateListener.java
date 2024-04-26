package com.starter.telegram.listener;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Update;
import com.starter.domain.entity.Bill.BillStatus;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.GroupRepository;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;


@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackQueryUpdateListener implements UpdateListener {

    public static final String ID_SEPARATOR = "_";
    public static final String CONFIRM_BILL_PREFIX = "confirm_bill_";
    public static final String ADDME_ACCEPT_PREFIX = "addme_accept_";
    public static final String ADDME_REJECT_PREFIX = "addme_reject_";

    private final BillRepository billRepository;
    private final GroupRepository groupRepository;
    private final UserInfoRepository userInfoRepository;
    private final TelegramMessageRenderer renderer;

    @Override
    @Transactional
    public void processUpdate(Update update, TelegramBot bot) {
        final var callbackQuery = update.callbackQuery();
        final var chatId = callbackQuery.from().id();
        if (callbackQuery.data().startsWith(CONFIRM_BILL_PREFIX)) {
            confirmBill(bot, callbackQuery, chatId);
        } else if (callbackQuery.data().startsWith(ADDME_ACCEPT_PREFIX)) {
            acceptAddMe(bot, callbackQuery, chatId);
        } else if (callbackQuery.data().startsWith(ADDME_REJECT_PREFIX)) {
            rejectAddMe(bot, callbackQuery, chatId);
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

    private void acceptAddMe(TelegramBot bot, CallbackQuery callbackQuery, Long chatId) {
        final var message = callbackQuery.maybeInaccessibleMessage();
        final var removedPrefix = callbackQuery.data().substring(ADDME_ACCEPT_PREFIX.length());
        final var parts = removedPrefix.split(ID_SEPARATOR);
        final var userChatId = Long.valueOf(parts[0]);
        final var groupChatId = Long.valueOf(parts[1]);
        final var userInfo = userInfoRepository.findByTelegramChatId(userChatId).orElseThrow();
        final var group = groupRepository.findByChatId(groupChatId).orElseThrow();
        if (!group.contains(userInfo.getUser())) {
            final var members = new LinkedList<>(group.getMembers());
            members.add(userInfo.getUser());
            group.setMembers(members);
            groupRepository.save(group);
            log.info("User {} added to group {}", userInfo, group);
        }
        final var messageUpdate = renderer.renderAddMeAcceptedUpdate(chatId, message, userInfo, group);
        bot.execute(messageUpdate);
    }

    private void rejectAddMe(TelegramBot bot, CallbackQuery callbackQuery, Long chatId) {
        final var message = callbackQuery.maybeInaccessibleMessage();
        final var messageUpdate = renderer.renderAddMeRejectedUpdate(chatId, message);
        bot.execute(messageUpdate);
    }
}
