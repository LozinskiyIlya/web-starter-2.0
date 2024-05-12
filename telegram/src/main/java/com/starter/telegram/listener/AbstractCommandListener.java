package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.data.util.Pair;

public abstract class AbstractCommandListener implements UpdateListener {

    protected Pair<String, String> parseCommand(String commandWithParameters) {
        String[] commandParts = commandWithParameters.split(" ");
        if (commandParts.length == 1) {
            return Pair.of(commandParts[0], "");
        }
        return Pair.of(commandParts[0], commandParts[1]);
    }

    protected void onUnknownCommand(Update update, TelegramBot bot, String command) {
        bot.execute(new SendMessage(update.message().chat().id(), "Unknown command: " + command));
    }
}
