package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.starter.telegram.service.TelegramUserService;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

import static com.starter.telegram.service.TelegramBotService.latestKeyboard;
import static com.starter.telegram.service.render.TelegramStaticRenderer.renderPin;


@Slf4j
@Component
@RequiredArgsConstructor
public class PrivateChatCommandListener extends AbstractCommandListener {

    private final TelegramUserService telegramUserService;
    private final TelegramMessageRenderer messageRenderer;
    public static final String START_COMMAND = "/start";
    public static final String TUTORIAL_COMMAND = "/tutorial";
    public static final String SETTINGS_COMMAND = "/settings";
    public static final String PIN_COMMAND = "/pin";
    public static final Set<String> COMMANDS = Set.of(START_COMMAND, TUTORIAL_COMMAND, PIN_COMMAND, SETTINGS_COMMAND);

    @Override
    public void processUpdate(Update update, TelegramBot bot) {
        final var commandParts = parseCommand(update.message().text());
        switch (commandParts.getFirst()) {
            case START_COMMAND -> onStartCommand(update, bot, commandParts.getSecond());
            case SETTINGS_COMMAND -> onSettingsCommand(update, bot);
            case TUTORIAL_COMMAND -> onTutorialCommand(update, bot);
            case PIN_COMMAND -> onPinCommand(update, bot, commandParts.getSecond());
            default -> onUnknownCommand(update, bot, commandParts.getFirst());
        }
        log.info("Received command: {} parameter: {}", commandParts.getFirst(), commandParts.getSecond());
    }

    private void onPinCommand(Update update, TelegramBot bot, String pinParameter) {
        final var chatId = update.message().chat().id();
        if (!StringUtils.hasText(pinParameter)) {
            bot.execute(renderPin(chatId));
        } else {
            if (pinParameter.length() != 6) {
                bot.execute(new SendMessage(chatId, "Pin code must be 6 characters long"));
            } else if (!pinParameter.matches("[0-9]+")) {
                bot.execute(new SendMessage(chatId, "Pin code must contain only numbers"));
            } else {
                final var userSettings = telegramUserService.createOrFindUserSettings(chatId);
                userSettings.setPinCode(pinParameter);
                telegramUserService.saveUserSettings(userSettings);
                bot.execute(new DeleteMessage(chatId, update.message().messageId()));
                bot.execute(new SendMessage(chatId, "Pin code saved"));
            }
        }
    }

    private void onStartCommand(Update update, TelegramBot bot, String startParameter) {
        // Create a reply keyboard that remains visible after use
        final var keyboard = latestKeyboard();
        // Send a message with the reply keyboard
        final var from = update.message().from();
        telegramUserService.createOrFindUser(from, bot);
        final var message = new SendMessage(from.id(), """
                                Hello! Thanks for using a bot!
                You can use the following commands:
                /tutorial - how to use the bot
                /settings - change your preferences
                /pin - set a pin code
                                """)
                .replyMarkup(keyboard);
        bot.execute(message);
    }

    private void onSettingsCommand(Update update, TelegramBot bot) {
        final var chatId = update.message().chat().id();
        final var message = messageRenderer.renderSettings(chatId);
        bot.execute(message);
    }

    private void onTutorialCommand(Update update, TelegramBot bot) {
        bot.execute(new SendMessage(update.message().chat().id(), "This is a simple bot"));
    }
}
