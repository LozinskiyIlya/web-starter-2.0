package com.starter.telegram.service;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.KeyboardButton;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.starter.telegram.listener.*;
import com.starter.telegram.listener.query.CallbackQueryUpdateListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.starter.telegram.service.render.TelegramStaticRenderer.randomExample;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService {
    public static final String NEW_BILL_BUTTON = "➕ NEW BILL";
    public static final String LATEST_BILLS = "Latest Bills  \uD83E\uDDFE";
    public static final String GROUPS = "Groups  \uD83D\uDC65";
    public static final String SETTINGS = "Settings  ⚙\uFE0F";
    private static final List<String> KEYBOARD_BUTTONS = List.of(LATEST_BILLS, GROUPS, SETTINGS);
    private final Map<Class<? extends UpdateListener>, UpdateListener> listeners = new HashMap<>();
    private final ExecutorService updatesExecutor = Executors.newFixedThreadPool(4);

    private final TelegramBot bot;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @PreDestroy
    void stop() {
        final var shutdownBotExecutor = Executors.newSingleThreadExecutor();
        shutdownBotExecutor.submit(() -> {
            bot.removeGetUpdatesListener();
            bot.shutdown();
            log.info("Bot stopped");
        });
        shutdownBotExecutor.shutdown();
        updatesExecutor.shutdown();
        try {
            shutdownBotExecutor.awaitTermination(15, TimeUnit.SECONDS);
            updatesExecutor.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            log.error("Failed to stop bot", ex);
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings("unchecked")
    @Autowired
    private void setListenersMap(Collection<UpdateListener> listeners) {
        for (UpdateListener listener : listeners) {
            Class<?> listenerClass = listener.getClass();
            // If the listener is a CGLIB proxy, get the superclass (the original class)
            if (org.springframework.aop.support.AopUtils.isCglibProxy(listener)) {
                listenerClass = listenerClass.getSuperclass();
            }
            this.listeners.put((Class<? extends UpdateListener>) listenerClass, listener);
        }
    }

    @PostConstruct
    public void start() {
        bot.setUpdatesListener(updates -> {
            updates.stream().map(UpdateWrapper::new).forEach(update ->
                    updatesExecutor.submit(() -> {
                        try {
                            if (update.message() != null && update.message().from().isBot()) {
                                log.warn("Ignoring bot message: {}", update.message().text());
                                return;
                            }
                            final var listener = selectListener(update);
                            listener.processUpdate(update, bot);
                        } catch (Exception e) {
                            log.error("Error while processing update: {}, updateObj: {}", e.getMessage(), update);
                        }
                    }));
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
            if (message.text() != null && message.text().startsWith("/")) {
                return listeners.get(GroupCommandListener.class);
            }
            return listeners.get(GroupUpdateListener.class);
        }
        if (message.location() != null) {
            return listeners.get(LocationUpdateListener.class);
        }
        if (message.text() != null && message.text().startsWith("/")) {
            return listeners.get(PrivateChatCommandListener.class);
        }
        if (message.text() != null && KEYBOARD_BUTTONS.contains(message.text())) {
            return listeners.get(KeyboardButtonUpdateListener.class);
        }
        if (message.text() != null || message.photo().length > 0 || message.document() != null) {
            return listeners.get(PrivateChatTextUpdateListener.class);
        }
        return listeners.get(NoopUpdateListener.class);
    }

    public static Keyboard latestKeyboard() {
        return new ReplyKeyboardMarkup(
                KEYBOARD_BUTTONS
                        .stream()
                        .map(KeyboardButton::new)
                        .map(button -> new KeyboardButton[]{button})
                        .toArray(KeyboardButton[][]::new))
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .inputFieldPlaceholder(randomExample());
    }
}
