package com.starter.telegram.listener;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.PinChatMessage;
import com.starter.common.events.BillConfirmedEvent;
import com.starter.domain.entity.Bill.BillStatus;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.GroupRepository;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.telegram.service.TelegramTutorialService;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.UUID;


@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackQueryUpdateListener implements UpdateListener {

    public static final String ID_SEPARATOR = "_";
    public static final String CONFIRM_BILL_PREFIX = "confirm_bill_";
    public static final String SKIP_BILL_PREFIX = "skip_bill_";
    public static final String ADDME_ACCEPT_PREFIX = "addme_accept_";
    public static final String ADDME_REJECT_PREFIX = "addme_reject_";
    public static final String RECOGNIZE_BILL_PREFIX = "recognize_";
    public static final String TUTORIAL_NEXT_PREFIX = "tutorial_next_";
    public static final String TUTORIAL_PREV_PREFIX = "tutorial_prev_";
    public static final String TUTORIAL_PIN_PREFIX = "tutorial_pin_";
    public static final String DEBUG_DAILY_REMINDER_PREFIX = "DEBUG_daily_reminder_";
    public static final String DEBUG_NO_BILLS_STATS_PREFIX = "DEBUG_no_bills_stats_";


    private final BillRepository billRepository;
    private final GroupRepository groupRepository;
    private final UserInfoRepository userInfoRepository;
    private final TelegramMessageRenderer renderer;
    private final ApplicationEventPublisher publisher;
    private final TelegramTutorialService tutorialService;

    @Override
    public void processUpdate(Update update, TelegramBot bot) {
        final var callbackQuery = update.callbackQuery();
        final var chatId = callbackQuery.from().id();
        final var callbackData = callbackQuery.data();
        if (callbackData.startsWith(CONFIRM_BILL_PREFIX)) {
            confirmBill(callbackQuery);
        } else if (callbackData.startsWith(SKIP_BILL_PREFIX)) {
            skipBill(bot, callbackQuery, chatId);
        } else if (callbackData.startsWith(ADDME_ACCEPT_PREFIX)) {
            acceptAddMe(bot, callbackQuery, chatId);
        } else if (callbackData.startsWith(ADDME_REJECT_PREFIX)) {
            rejectAddMe(bot, callbackQuery, chatId);
        } else if (callbackData.startsWith(RECOGNIZE_BILL_PREFIX)) {
            bot.execute(renderer.renderRecognizeMyBill(chatId));
        } else if (callbackData.startsWith(TUTORIAL_NEXT_PREFIX) || callbackData.startsWith(TUTORIAL_PREV_PREFIX)) {
            tutorialService.onStepChanged(update, bot);
        } else if (callbackData.startsWith(TUTORIAL_PIN_PREFIX)) {
            tutorialService.onPinTutorial(update, bot);
        } else if (callbackData.startsWith(DEBUG_DAILY_REMINDER_PREFIX)) {
            final var userInfo = userInfoRepository.findByTelegramChatId(chatId).orElseThrow();
            final var userSettings = userInfo.getUser().getUserSettings();
            final var message = renderer.renderDailyReminder(userSettings);
            bot.execute(message);
        } else if (callbackData.startsWith(DEBUG_NO_BILLS_STATS_PREFIX)) {
            final var message = renderer.renderNoBills(chatId, "July");
            bot.execute(message);
        }
    }

    private void confirmBill(CallbackQuery callbackQuery) {
        final var billId = UUID.fromString(callbackQuery.data().substring(CONFIRM_BILL_PREFIX.length()));
        publisher.publishEvent(new BillConfirmedEvent(this, billId));
    }

    private void skipBill(TelegramBot bot, CallbackQuery callbackQuery, Long chatId) {
        final var message = callbackQuery.maybeInaccessibleMessage();
        final var billId = UUID.fromString(callbackQuery.data().substring(SKIP_BILL_PREFIX.length()));
        final var bill = billRepository.findById(billId).orElseThrow();
        bill.setStatus(BillStatus.SKIPPED);
        billRepository.save(bill);
        log.info("Bill skipped: {}", bill);
        final var messageUpdate = renderer.renderBillSkipped(chatId, bill, message);
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
