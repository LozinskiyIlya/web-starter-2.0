package com.starter.telegram.service;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.starter.telegram.service.listener.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService {

    public static final Set<String> KEYBOARD_BUTTONS = Set.of();

    private final TelegramBot bot;
    private final Map<Class<? extends UpdateListener>, UpdateListener> listeners = new HashMap<>();

    @PreDestroy
    void stop() {
        final var shutdownBotExecutor = Executors.newSingleThreadExecutor();
        shutdownBotExecutor.submit(() -> {
            bot.removeGetUpdatesListener();
            bot.shutdown();
            log.info("Bot stopped");
        });
        shutdownBotExecutor.shutdown();
        try {
            shutdownBotExecutor.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            log.error("Failed to stop bot", ex);
            Thread.currentThread().interrupt();
        }
    }

    @Autowired
    private void setListenersMap(Collection<UpdateListener> listeners) {
        listeners.forEach(listener -> this.listeners.put(listener.getClass(), listener));
    }

    @PostConstruct
    public void start() {
        bot.setUpdatesListener(updates -> {
            updates.forEach(update -> {
                try {
                    final var listener = selectListener(update);
                    listener.processUpdate(update, bot);
                } catch (Exception e) {
                    log.error("Error while processing update: {}, updateObj: {}", e.getMessage(), update);
                }
            });
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, e -> log.error("Error while processing updates: {}", e.toString()));
        log.info("Bot started");
    }

    private UpdateListener selectListener(Update update) {
        if (update.callbackQuery() != null) {
            return listeners.get(CallbackQueryUpdateListener.class);
        }
        final var message = update.message();
        if (message == null) {
            return listeners.get(NoopUpdateListener.class);
        }
        final var chatType = message.chat().type();
        if ("group".equals(chatType.name()) || "supergroup".equals(chatType.name())) {
            return listeners.get(GroupUpdateListener.class);
        }
        if (message.location() != null) {
            return listeners.get(LocationUpdateListener.class);
        }
        if (message.text() != null && message.text().startsWith("/")) {
            return listeners.get(CommandUpdateListener.class);
        }
        if (message.text() != null && KEYBOARD_BUTTONS.contains(message.text())) {
            return listeners.get(KeyboardButtonUpdateListener.class);
        }
        return listeners.get(NoopUpdateListener.class);
    }

}
