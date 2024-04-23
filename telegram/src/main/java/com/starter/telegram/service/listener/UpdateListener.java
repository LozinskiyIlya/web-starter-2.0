package com.starter.telegram.service.listener;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;

public interface UpdateListener {

    void processUpdate(Update update, final TelegramBot bot);

}
