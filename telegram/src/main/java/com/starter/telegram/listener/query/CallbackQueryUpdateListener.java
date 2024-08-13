package com.starter.telegram.listener.query;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.starter.telegram.listener.UpdateListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.starter.telegram.service.render.TelegramStaticRenderer.renderRecognizeMyBill;


@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackQueryUpdateListener implements UpdateListener {

    public static final String QUERY_SEPARATOR = "_";
    public static final String RECOGNIZE_BILL_PREFIX = "recognize_";

    private final Map<String, CallbackExecutor> executors = new HashMap<>();

    @Autowired
    private void setExecutors(List<CallbackExecutor> executors) {
        executors.forEach(executor -> {
            if (this.executors.containsKey(executor.getPrefix())) {
                throw new IllegalArgumentException("Duplicate prefix found in callback executors: " + executor.getPrefix());
            }
            this.executors.put(executor.getPrefix(), executor);
        });
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
                .ifPresentOrElse(callbackExecutor -> callbackExecutor.execute(bot, callbackQuery, chatId),
                        () -> processLocally(bot, callbackQuery, chatId));
        bot.execute(new AnswerCallbackQuery(callbackQuery.id()));
    }

    private void processLocally(TelegramBot bot, CallbackQuery query, Long chatId) {
        final var callbackData = query.data();
        if (callbackData.startsWith(RECOGNIZE_BILL_PREFIX)) {
            bot.execute(renderRecognizeMyBill(chatId));
        } else {
            log.warn("Unknown callback query: {} for chatId: {}", callbackData, chatId);
        }
    }
}