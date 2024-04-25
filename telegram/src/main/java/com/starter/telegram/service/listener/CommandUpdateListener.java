package com.starter.telegram.service.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.starter.telegram.service.TelegramUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class CommandUpdateListener implements UpdateListener {

    private final TelegramUserService telegramUserService;

    @Override
    public void processUpdate(Update update, TelegramBot bot) {
        final var commandParts = parseCommand(update.message().text());
        switch (commandParts.getFirst()) {
            case "/start":
                onStartCommand(update, bot, commandParts.getSecond());
                break;
            case "/info":
                onInfoCommand(update, bot);
                break;
            default:
                onUnknownCommand(update, bot, commandParts.getFirst());
        }
        log.info("Received command: {} parameter: {}", commandParts.getFirst(), commandParts.getSecond());
    }

    private void onUnknownCommand(Update update, TelegramBot bot, String command) {
        bot.execute(new SendMessage(update.message().chat().id(), "Unknown command: " + command));
    }

    private void onStartCommand(Update update, TelegramBot bot, String startParameter) {
        // Create a reply keyboard that remains visible after use
        final var keyboard = new ReplyKeyboardMarkup(
                new KeyboardButton("Button 1").requestLocation(true))
                .addRow(new KeyboardButton("Button 2"))
                .addRow(new KeyboardButton("Button 3"))
                .resizeKeyboard(true)
                .oneTimeKeyboard(false);
        // Send a message with the reply keyboard
        Long chatId = update.message().chat().id();
        telegramUserService.createOrFindUser(update);
        final var message = new SendMessage(chatId, "Hello! Thanks for using a bot!")
                .replyMarkup(keyboard);
        bot.execute(message);
    }


    private void onInfoCommand(Update update, TelegramBot bot) {
        bot.execute(new SendMessage(update.message().chat().id(), "This is a simple bot"));
    }

    private Pair<String, String> parseCommand(String commandWithParameters) {
        String[] commandParts = commandWithParameters.split(" ");
        if (commandParts.length == 1) {
            return Pair.of(commandParts[0], "");
        }
        return Pair.of(commandParts[0], commandParts[1]);
    }
}
