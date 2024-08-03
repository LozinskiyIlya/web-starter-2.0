package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.starter.common.service.HttpService;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.telegram.service.TelegramTutorialService;
import com.starter.telegram.service.TelegramUserService;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.UUID;

import static com.starter.telegram.service.render.TelegramStaticRenderer.renderPin;
import static com.starter.telegram.service.render.TelegramStaticRenderer.tryUpdateMessage;


@Slf4j
@Component
@RequiredArgsConstructor
public class PrivateChatCommandListener extends AbstractCommandListener {

    private final TelegramUserService telegramUserService;
    private final TelegramTutorialService tutorialService;
    private final TelegramMessageRenderer messageRenderer;
    private final UserInfoRepository userInfoRepository;
    private final HttpService httpService;

    @Value("${starter.chat-with-bills.host}")
    private String chatWithBillsHost;

    @Value("${starter.chat-with-bills.port}")
    private String chatWithBillsPort;
    public static final String START_COMMAND = "/start";
    public static final String TUTORIAL_COMMAND = "/tutorial";
    public static final String PIN_COMMAND = "/pin";
    public static final String HELP_COMMAND = "/help";
    public static final String CHAT_WITH_BILLS_COMMAND = "/chat";
    public static final String EMPTY_COMMAND = "/empty";
    public static final Set<String> COMMANDS = Set.of(START_COMMAND, TUTORIAL_COMMAND, PIN_COMMAND, HELP_COMMAND, CHAT_WITH_BILLS_COMMAND, EMPTY_COMMAND);

    @Override
    public void processUpdate(Update update, TelegramBot bot) {
        final var commandParts = parseCommand(update.message().text());
        switch (commandParts.getFirst()) {
            case START_COMMAND -> onStartCommand(update, bot, commandParts.getSecond());
            case HELP_COMMAND -> onHelpCommand(update, bot);
            case TUTORIAL_COMMAND -> onTutorialCommand(update, bot);
            case PIN_COMMAND -> onPinCommand(update, bot, commandParts.getSecond());
            case CHAT_WITH_BILLS_COMMAND -> onChatCommand(update, bot, commandParts.getSecond());
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
        final var from = update.message().from();
        telegramUserService.createOrFindUser(from, bot);
        final var message = messageRenderer.renderStartMessage(from.id(), from.firstName());
        bot.execute(message);
    }

    private void onHelpCommand(Update update, TelegramBot bot) {
        final var from = update.message().from();
        final var message = messageRenderer.renderHelp(from.id());
        bot.execute(message);
    }

    private void onTutorialCommand(Update update, TelegramBot bot) {
        tutorialService.onTutorialCommand(update, bot);
    }

    private void onChatCommand(Update update, TelegramBot bot, String query) {
        // todo: test
        final var chatId = update.message().chat().id();
        if (!StringUtils.hasText(query)) {
            final var message = messageRenderer.renderChatWithBillsUsage(chatId);
            bot.execute(message);
            return;
        }
        final var userInfo = userInfoRepository.findByTelegramChatId(chatId).orElseThrow();
        final var request = new ChatWithBillsRequest(userInfo.getUser().getId(), query);
        final var processingMessage = bot.execute(new SendMessage(chatId, "Processing your query...")).message();
        try {
            final var url = chatWithBillsHost + ":" + chatWithBillsPort + "/chat";
            final var response = httpService.postT(url, request, ChatWithBillsResponse.class);
            bot.execute(tryUpdateMessage(chatId, processingMessage, response.getOutput()));
        } catch (Exception e) {
            log.error("Error while processing chat command", e);
            bot.execute(tryUpdateMessage(chatId, processingMessage, "Error while processing chat command, please try again"));
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatWithBillsRequest {
        private UUID ownerId;
        private String query;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatWithBillsResponse {
        private String input;
        private String output;
    }
}
