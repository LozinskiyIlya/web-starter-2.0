package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;


@Component
public class NoopUpdateListener implements UpdateListener {
    @Override
    public void processUpdate(Update update, final TelegramBot bot) {
        bot.execute(new SendMessage(update.message().chat().id(), "Please use one of the available commands or buttons to interact with the bot."));
    }
}
