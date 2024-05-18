package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.starter.telegram.service.TelegramBotService.NEW_BILL_BUTTON;


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
            default -> {
            }
        }
    }

}
