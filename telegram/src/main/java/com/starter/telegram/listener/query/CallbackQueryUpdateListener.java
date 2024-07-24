package com.starter.telegram.listener.query;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.telegram.listener.UpdateListener;
import com.starter.telegram.service.TelegramTutorialService;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackQueryUpdateListener implements UpdateListener {

    public static final String QUERY_SEPARATOR = "_";
    public static final String RECOGNIZE_BILL_PREFIX = "recognize_";
    public static final String TUTORIAL_NEXT_PREFIX = "tutorial_next_";
    public static final String TUTORIAL_PREV_PREFIX = "tutorial_prev_";
    public static final String TUTORIAL_PIN_PREFIX = "tutorial_pin_";
    public static final String DEBUG_DAILY_REMINDER_PREFIX = "DEBUG_daily_reminder_";
    public static final String DEBUG_NO_BILLS_STATS_PREFIX = "DEBUG_no_bills_stats_";


    private final UserInfoRepository userInfoRepository;
    private final TelegramMessageRenderer renderer;
    private final TelegramTutorialService tutorialService;
    private final Map<String, CallbackExecutor> executors = new HashMap<>();

    @Autowired
    private void setExecutors(List<CallbackExecutor> executors) {
        executors.forEach(executor -> this.executors.put(executor.getPrefix(), executor));
    }


    @Override
    public void processUpdate(Update update, TelegramBot bot) {
        final var callbackQuery = update.callbackQuery();
        final var chatId = callbackQuery.from().id();
        final var callbackData = callbackQuery.data();
        executors.keySet()
                .stream()
                .filter(callbackData::startsWith)
                .findFirst()
                .map(executors::get)
                .ifPresent(callbackExecutor -> callbackExecutor.execute(bot, callbackQuery, chatId));
        if (callbackData.startsWith(RECOGNIZE_BILL_PREFIX)) {
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
}
