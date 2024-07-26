package com.starter.telegram.listener.query;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;

import java.util.UUID;

public interface CallbackExecutor {

    void execute(TelegramBot bot, CallbackQuery query, Long chatId);

    String getPrefix();

    default UUID extractId(CallbackQuery query, String prefix) {
        return UUID.fromString(query.data().substring(prefix.length()));
    }
}
