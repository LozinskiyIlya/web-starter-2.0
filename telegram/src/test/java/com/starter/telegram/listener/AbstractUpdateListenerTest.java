package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.*;
import com.pengrad.telegrambot.request.BaseRequest;
import com.starter.telegram.configuration.TelegramProperties;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest
public abstract class AbstractUpdateListenerTest {

    @Autowired
    protected TransactionTemplate transactionTemplate;
    @Autowired
    protected TelegramProperties telegramProperties;
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

    protected Update mockGroupUpdate(String text, Long userChatId, Long groupChatId) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        User user = mockReturnedUserData(userChatId);
        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(message.from()).thenReturn(user);
        when(chat.id()).thenReturn(groupChatId);
        when(chat.title()).thenReturn(UUID.randomUUID().toString());
        when(message.text()).thenReturn(text);
        return update;
    }

    protected Update mockCallbackQueryUpdate(String data, Long chatId) {
        Update update = mock(Update.class);
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        User user = mockReturnedUserData(chatId);
        when(update.callbackQuery()).thenReturn(callbackQuery);
        when(callbackQuery.data()).thenReturn(data);
        when(callbackQuery.from()).thenReturn(user);
        return update;
    }

    protected User mockBotUser() {
        com.pengrad.telegrambot.model.User user = mock(com.pengrad.telegrambot.model.User.class);
        when(user.isBot()).thenReturn(true);
        when(user.username()).thenReturn(telegramProperties.getUsername());
        return user;
    }

    protected static User mockReturnedUserData(Long chatId) {
        com.pengrad.telegrambot.model.User user = mock(com.pengrad.telegrambot.model.User.class);
        when(user.id()).thenReturn(chatId);
        when(user.username()).thenReturn("username");
        when(user.firstName()).thenReturn("firstName");
        when(user.lastName()).thenReturn("lastName");
        when(user.languageCode()).thenReturn("en");
        when(user.isPremium()).thenReturn(true);
        return user;
    }

    @SuppressWarnings("unchecked")
    protected static BaseRequest<?, ?> assertMessageSentToChatId(TelegramBot bot, Long chatId) {
        final var captor = ArgumentCaptor.forClass(BaseRequest.class);
        verify(bot, atLeast(1)).execute(captor.capture());
        final var actualRequest = captor.getValue();
        final var sendTo = actualRequest.getParameters().get("chat_id").toString();
        assertTrue(sendTo.contains(chatId.toString()));
        return actualRequest;
    }

    @SuppressWarnings("unchecked")
    protected static BaseRequest<?, ?> assertSentMessageContainsText(TelegramBot bot, String shouldContain) {
        final var captor = ArgumentCaptor.forClass(BaseRequest.class);
        Mockito.verify(bot).execute(captor.capture());
        final var actualRequest = captor.getValue();
        assertTrue(actualRequest.getParameters().get("text").toString().contains(shouldContain));
        return actualRequest;
    }
}
