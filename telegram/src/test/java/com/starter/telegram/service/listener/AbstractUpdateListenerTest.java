package com.starter.telegram.service.listener;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.*;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
public abstract class AbstractUpdateListenerTest {

    @Autowired
    protected TransactionTemplate transactionTemplate;
    protected final EasyRandom random = new EasyRandom(new EasyRandomParameters().seed(System.nanoTime()));

    protected TelegramBot mockBot() {
        final var bot = Mockito.mock(TelegramBot.class);
        when(bot.execute(Mockito.any())).thenReturn(null);
        return bot;
    }

    protected Update mockCommandUpdate(String command, Long chatId) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        User user = mockReturnedUserData(chatId);
        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(message.from()).thenReturn(user);
        when(chat.id()).thenReturn(chatId);
        when(message.text()).thenReturn(command);
        return update;
    }

    protected Update mockGroupUpdate(String text, Long userChatId) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        User user = mockReturnedUserData(userChatId);
        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(message.from()).thenReturn(user);
        when(chat.id()).thenReturn(random.nextLong());
        when(chat.title()).thenReturn(UUID.randomUUID().toString());
        when(message.text()).thenReturn(text);
        return update;
    }

    protected Update mockCallbackQueryUpdate(UUID spotId, Long chatId) {
        Update update = mock(Update.class);
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        User user = mockReturnedUserData(chatId);
        when(update.callbackQuery()).thenReturn(callbackQuery);
        when(callbackQuery.data()).thenReturn(spotId.toString());
        when(callbackQuery.from()).thenReturn(user);
        return update;
    }

    private static User mockReturnedUserData(Long chatId) {
        com.pengrad.telegrambot.model.User user = mock(com.pengrad.telegrambot.model.User.class);
        when(user.id()).thenReturn(chatId);
        when(user.username()).thenReturn("username");
        when(user.firstName()).thenReturn("firstName");
        when(user.lastName()).thenReturn("lastName");
        when(user.languageCode()).thenReturn("en");
        when(user.isPremium()).thenReturn(true);
        return user;
    }
}
