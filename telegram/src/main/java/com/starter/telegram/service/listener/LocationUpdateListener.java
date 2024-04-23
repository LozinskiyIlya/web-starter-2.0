package com.starter.telegram.service.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocationUpdateListener implements UpdateListener {

    @Override
    public void processUpdate(Update update, TelegramBot bot) {
        final var location = update.message().location();
        final var chatId = update.message().chat().id();
        log.info("Received location: {}, {}", location.latitude(), location.longitude());
    }

}
