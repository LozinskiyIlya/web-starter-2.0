package com.starter.telegram.service.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class KeyboardButtonUpdateListener implements UpdateListener {

    @Value("${starter.server.host:localhost}")
    private String host;

    @Override
    public void processUpdate(Update update, TelegramBot bot) {
        final var buttonPressed = update.message().text();
        final var chatId = update.message().chat().id();
        switch (buttonPressed) {
            default:
                break;
        }
    }

}
