package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.starter.telegram.service.TelegramUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class PrivateChatCommandListener extends AbstractCommandListener {

    private final TelegramUserService telegramUserService;

    @Override
    public void processUpdate(Update update, TelegramBot bot) {
        final var commandParts = parseCommand(update.message().text());
        switch (commandParts.getFirst()) {
            case "/start" -> onStartCommand(update, bot, commandParts.getSecond());
            case "/info" -> onInfoCommand(update, bot);
            default -> onUnknownCommand(update, bot, commandParts.getFirst());
        }
        log.info("Received command: {} parameter: {}", commandParts.getFirst(), commandParts.getSecond());
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
        final var from = update.message().from();
        telegramUserService.createOrFindUser(from, bot);
        final var message = new SendMessage(from.id(), "Hello! Thanks for using a bot!")
                .replyMarkup(keyboard);
        bot.execute(message);
    }


    private void onInfoCommand(Update update, TelegramBot bot) {
        bot.execute(new SendMessage(update.message().chat().id(), "This is a simple bot"));
    }
}
