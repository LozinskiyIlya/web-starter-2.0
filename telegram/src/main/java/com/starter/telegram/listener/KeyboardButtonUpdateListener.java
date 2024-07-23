package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.starter.telegram.service.TelegramBotService.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeyboardButtonUpdateListener implements UpdateListener {

    private final TelegramMessageRenderer renderer;

    @Override
    public void processUpdate(Update update, TelegramBot bot) {
        final var buttonPressed = update.message().text();
        final var chatId = update.message().chat().id();
        switch (buttonPressed) {
            case NEW_BILL_BUTTON -> {
                final var message = renderer.renderNewBill(chatId);
                bot.execute(message);
            }
            case THIS_MONTH -> onTodayStats(chatId, bot);
            case GROUPS -> onMyGroups(chatId, bot);
            case HELP -> onHelp(chatId, bot);
            default -> {
            }
        }
    }

    private void onTodayStats(Long chatId, TelegramBot bot) {
        final var message = renderer.renderSettings(chatId);
        bot.execute(message);
    }

    private void onMyGroups(Long chatId, TelegramBot bot) {
        final var message = renderer.renderSettings(chatId);
        bot.execute(message);
    }

    private void onHelp(Long chatId, TelegramBot bot) {
        final var message = renderer.renderSettings(chatId);
        bot.execute(message);
    }

}
