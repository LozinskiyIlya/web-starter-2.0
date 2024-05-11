package com.starter.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.*;
import com.pengrad.telegrambot.request.BaseRequest;
import com.starter.domain.repository.testdata.BillTestDataCreator;
import com.starter.domain.repository.testdata.UserTestDataCreator;
import com.starter.telegram.configuration.TelegramProperties;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest
@ContextConfiguration(classes = AbstractTelegramTest.AbstractUpdateListenerTestConfig.class)
public abstract class AbstractTelegramTest {

    @Autowired
    protected BillTestDataCreator billTestDataCreator;

    @Autowired
    protected UserTestDataCreator userTestDataCreator;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    @Autowired
    protected TelegramProperties telegramProperties;

    @Autowired
    protected TelegramBot bot;

    @BeforeEach
    void setUp() {
        clearInvocations(bot);
    }

    protected final EasyRandom random = new EasyRandom(new EasyRandomParameters().seed(System.nanoTime()));

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

    protected TelegramBot mockBot() {
        return bot;
    }

    @SuppressWarnings("unchecked")
    protected static void assertMessageSentToChatId(TelegramBot bot, Long chatId) {
        final var captor = ArgumentCaptor.forClass(BaseRequest.class);
        verify(bot, atLeastOnce()).execute(captor.capture());
        final var foundIds = new LinkedList<String>();
        final var actualRequests = captor.getAllValues();
        final var wasSent = actualRequests.stream()
                .map(BaseRequest::getParameters)
                .map(params -> params.get("chat_id"))
                .map(Object::toString)
                .peek(foundIds::add)
                .anyMatch(chatId.toString()::equals);
        assertTrue(wasSent, "Message to " + chatId + " was not found. Present ids: " + foundIds);
    }

    @SuppressWarnings("unchecked")
    protected static void assertSentMessageContainsText(TelegramBot bot, String shouldContain) {
        final var captor = ArgumentCaptor.forClass(BaseRequest.class);
        Mockito.verify(bot, atLeastOnce()).execute(captor.capture());
        final var foundTexts = new LinkedList<String>();
        final var actualRequests = captor.getAllValues();
        final var contains = actualRequests.stream()
                .map(BaseRequest::getParameters)
                .map(params -> params.get("text"))
                .map(Object::toString)
                .peek(foundTexts::add)
                .anyMatch(text -> text.contains(shouldContain));
        assertTrue(contains, "Message containing: \"" + shouldContain + "\" was not found. Present texts: " + foundTexts);
    }

    @TestConfiguration
    static class AbstractUpdateListenerTestConfig {
        @Bean
        @Primary
        protected TelegramBot mockBot() {
            final var bot = Mockito.mock(TelegramBot.class);
            when(bot.execute(Mockito.any())).thenReturn(null);
            return bot;
        }

        @Bean
        @Primary
        public SyncTaskExecutor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }
}
