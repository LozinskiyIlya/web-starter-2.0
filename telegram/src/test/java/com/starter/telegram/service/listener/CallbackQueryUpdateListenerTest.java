package com.starter.telegram.service.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Update;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@SpringBootTest
class CallbackQueryUpdateListenerTest {


    @Autowired
    private CallbackQueryUpdateListener listener;

    private final EasyRandom random = new EasyRandom(new EasyRandomParameters().seed(System.nanoTime()));


    private Update mockUpdate(UUID spotId, Long chatId) {
        Update update = mock(Update.class);
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        when(update.callbackQuery()).thenReturn(callbackQuery);
        com.pengrad.telegrambot.model.User user = mock(com.pengrad.telegrambot.model.User.class);
        when(callbackQuery.data()).thenReturn(spotId.toString());
        when(callbackQuery.from()).thenReturn(user);
        when(user.id()).thenReturn(chatId);
        when(user.username()).thenReturn("username");
        when(user.firstName()).thenReturn("firstName");
        when(user.lastName()).thenReturn("lastName");
        when(user.languageCode()).thenReturn("en");
        when(user.isPremium()).thenReturn(true);
        return update;
    }

    private TelegramBot mockBot() {
        final var bot = Mockito.mock(TelegramBot.class);
        when(bot.execute(Mockito.any())).thenReturn(null);
        return bot;
    }
}