package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;

import static com.starter.telegram.listener.query.CallbackQueryUpdateListener.DEBUG_DAILY_REMINDER_PREFIX;
import static com.starter.telegram.listener.query.CallbackQueryUpdateListener.DEBUG_NO_BILLS_STATS_PREFIX;
import static com.starter.telegram.service.TelegramBotService.latestKeyboard;

public abstract class AbstractCommandListener implements UpdateListener {

    protected static final String DEBUG_COMMAND = "/debug";

    @Value("${spring.profiles.active:Unknown}")
    private String activeProfile;

    protected Pair<String, String> parseCommand(String commandWithParameters) {
        String[] commandParts = commandWithParameters.split(" ");
        if (commandParts.length == 1) {
            return Pair.of(commandParts[0], "");
        }
        return Pair.of(commandParts[0], commandParts[1]);
    }

    protected void onUnknownCommand(Update update, TelegramBot bot, String command) {
        if ("local".equals(activeProfile) && command.equals(DEBUG_COMMAND)) {
            bot.execute(new SendMessage(update.message().chat().id(), "Debug actions:")
                    .replyMarkup(
                            new InlineKeyboardMarkup(
                                    new InlineKeyboardButton("Send daily reminder").callbackData(DEBUG_DAILY_REMINDER_PREFIX),
                                    new InlineKeyboardButton("Show no bills stats").callbackData(DEBUG_NO_BILLS_STATS_PREFIX)
                            )));
            return;
        }
        bot.execute(new SendMessage(update.message().chat().id(), "Unknown command: " + command).replyMarkup(latestKeyboard()));
    }
}
