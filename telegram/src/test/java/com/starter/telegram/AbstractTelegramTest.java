package com.starter.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.*;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.response.GetChatResponse;
import com.pengrad.telegrambot.response.GetFileResponse;
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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    protected Update mockPhotoUpdate(Update update, String filePath) {
        Message message = update.message();
        GetFileResponse fileResponse = mock(GetFileResponse.class);
        File file = mock(File.class);
        when(file.filePath()).thenReturn(filePath);
        when(fileResponse.file()).thenReturn(file);
        when(message.photo()).thenReturn(new PhotoSize[]{new PhotoSize()});
        when(bot.execute(any(GetFile.class))).thenReturn(fileResponse);
        when(bot.getFullFilePath(any(File.class))).thenReturn(filePath);
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

    protected static GetChatResponse mockReturnedChatData(Long chatId, String bio) {
        GetChatResponse response = mock(GetChatResponse.class);
        Chat chat = mock(Chat.class);
        ChatPhoto chatPhoto = mockChatPhoto();
        when(response.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        when(chat.bio()).thenReturn(bio);
        when(chat.photo()).thenReturn(chatPhoto);
        when(chat.birthdate()).thenReturn(new Birthdate());
        return response;
    }

    protected static ChatPhoto mockChatPhoto() {
        ChatPhoto photo = mock(ChatPhoto.class);
        when(photo.smallFileId()).thenReturn("AQADAgADy6cxGzECFxEACAIAAzECFxEABML2Cb7Tv3otNQQ");
        when(photo.bigFileId()).thenReturn("AQADAgADy6cxGzECFxEACAMAAzECFxEABML2Cb7Tv3otNQQ");
        return photo;
    }

    protected static void assertMessageNotSentToChatId(TelegramBot bot, Long chatId) {
        final var wasNotSend = getCapturedRequestParams(bot)
                .map(params -> params.get("chat_id"))
                .noneMatch(chatId::equals);
        assertTrue(wasNotSend, "Unexpected message to " + chatId + " was found");
    }

    protected static void assertMessageSentToChatId(TelegramBot bot, Long chatId) {
        final var foundIds = new LinkedList<>();
        final var wasSentTimes = getCapturedRequestParams(bot)
                .map(params -> params.get("chat_id"))
                .peek(foundIds::add)
                .filter(chatId::equals)
                .count();
        assertEquals(1, wasSentTimes, "Message to " + chatId + " was not found. Present ids: " + foundIds);
    }

    protected static void assertSentMessageContainsText(TelegramBot bot, String shouldContain) {
        final var foundTexts = new LinkedList<>();
        final var containsTimes = getCapturedRequestParams(bot)
                .map(params -> params.get("text"))
                .peek(foundTexts::add)
                .filter(text -> ((String) text).contains(shouldContain))
                .count();
        assertEquals(1, containsTimes, "Message containing: \"" + shouldContain + "\" was not found. Present texts: " + foundTexts);
    }

    protected static void assertSentMessageToChatIdContainsText(TelegramBot bot, Long chatId, String shouldContain) {
        assertSentMessageToChatIdContainsKey(bot, chatId, "text", shouldContain);
    }

    protected static void assertSentMessageToChatIdContainsKey(TelegramBot bot, Long chatId, String key, String value) {
        final var foundTexts = new LinkedList<>();
        final var containsTimes = getCapturedRequestParams(bot)
                .filter(params -> params.containsKey("chat_id"))
                .filter(params -> params.get("chat_id").equals(chatId))
                .map(params -> params.get(key))
                .filter(Objects::nonNull)
                .peek(foundTexts::add)
                .filter(text -> ((String) text).contains(value))
                .count();
        assertEquals(1, containsTimes, "Message containing: \"" + key + "\" = \"" + value + "\" was not found. Present texts: " + foundTexts);
    }

    protected static void assertSentMessageNotContainsText(TelegramBot bot, String shouldNotContain) {
        final var foundTexts = new LinkedList<>();
        final var containsTimes = getCapturedRequestParams(bot)
                .map(params -> params.get("text"))
                .peek(foundTexts::add)
                .filter(text -> ((String) text).contains(shouldNotContain))
                .count();
        assertEquals(0, containsTimes, "Message containing: \"" + shouldNotContain + "\" was found. Present texts: " + foundTexts);
    }

    protected static void assertSentMessageToChatIdContainsKeyboard(TelegramBot bot, Long chatId) {
        final var containsTimes = getCapturedRequestParams(bot)
                .filter(params -> params.get("chat_id").equals(chatId))
                .map(params -> params.get("reply_markup"))
                .filter(Objects::nonNull)
                .count();
        assertEquals(1, containsTimes, "Message attribute reply_markup was not found");
    }

    protected static void assertSendMessageToChatIdIsInSilentMode(TelegramBot bot, Long chatId) {
        final var silent = getCapturedRequestParams(bot)
                .filter(params -> params.get("chat_id").equals(chatId))
                .map(params -> params.get("disable_notification"))
                .allMatch(Boolean.class::cast);
        assertTrue(silent, "Message attribute disable_notification was not found or is not true");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Stream<Map> getCapturedRequestParams(TelegramBot bot) {
        final var captor = ArgumentCaptor.forClass(BaseRequest.class);
        verify(bot, atLeast(0)).execute(captor.capture());
        return captor.getAllValues()
                .stream()
                .map(BaseRequest::getParameters);
    }

    @TestConfiguration
    static class AbstractUpdateListenerTestConfig {
        @Bean
        @Primary
        protected TelegramBot telegramBot() {
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
