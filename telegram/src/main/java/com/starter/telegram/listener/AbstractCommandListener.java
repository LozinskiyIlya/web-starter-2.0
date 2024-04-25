package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import org.springframework.data.util.Pair;

public abstract class AbstractCommandListener implements UpdateListener {
    public abstract void processUpdate(Update update, TelegramBot bot);

    protected Pair<String, String> parseCommand(String commandWithParameters) {
        String[] commandParts = commandWithParameters.split(" ");
        if (commandParts.length == 1) {
            return Pair.of(commandParts[0], "");
        }
        return Pair.of(commandParts[0], commandParts[1]);
    }
}
