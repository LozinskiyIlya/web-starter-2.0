package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.starter.telegram.service.TelegramStateMachine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.starter.telegram.listener.PrivateChatCommandListener.EMPTY_COMMAND;
import static com.starter.telegram.listener.query.DebugCallbackExecutor.*;
import static com.starter.telegram.service.render.TelegramStaticRenderer.withLatestKeyboard;

public abstract class AbstractCommandListener implements UpdateListener {

    protected static final String DEBUG_COMMAND = "/debug";

    @Value("${spring.profiles.active:Unknown}")
    private String activeProfile;

    @Autowired
    private TelegramStateMachine stateMachine;

    protected Pair<String, String> parseCommand(String commandWithParameters) {
        String[] commandParts = commandWithParameters.split(" ");
        if (commandParts.length == 1) {
            return Pair.of(commandParts[0], "");
        }
        return Pair.of(commandParts[0], Arrays.stream(commandParts).skip(1).collect(Collectors.joining(" ")));
    }

    protected void onUnknownCommand(Update update, TelegramBot bot, String command) {
        final var chatId = update.message().chat().id();
        if (command.equals(EMPTY_COMMAND)) {
            onEmptyCommand(chatId, bot);
            return;
        }
        if ("local".equals(activeProfile) && command.equals(DEBUG_COMMAND)) {
            bot.execute(new SendMessage(chatId, "Debug actions:")
                    .replyMarkup(new InlineKeyboardMarkup()
                            .addRow(new InlineKeyboardButton("Send daily reminder").callbackData(DEBUG_DAILY_REMINDER_PREFIX))
                            .addRow(new InlineKeyboardButton("Send weekly report").callbackData(DEBUG_WEEKLY_REPORT_PREFIX))
                            .addRow(new InlineKeyboardButton("Show no bills stats").callbackData(DEBUG_NO_BILLS_STATS_PREFIX))
                    ));
            return;
        }
        bot.execute(withLatestKeyboard(chatId, "Unknown command: " + command));
    }

    protected void onEmptyCommand(Long chatId, TelegramBot bot) {
        stateMachine.removeState(chatId);
        bot.execute(withLatestKeyboard(chatId, "Current operation canceled"));
    }
}
