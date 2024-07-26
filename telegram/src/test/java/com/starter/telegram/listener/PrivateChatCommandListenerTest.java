package com.starter.telegram.listener;

import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.request.GetChat;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetChatResponse;
import com.starter.domain.entity.Role;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.domain.repository.UserSettingsRepository;
import com.starter.telegram.AbstractTelegramTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class PrivateChatCommandListenerTest extends AbstractTelegramTest {

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private PrivateChatCommandListener privateChatCommandListener;

    @Nested
    @DisplayName("on start command")
    class OnStartCommand {

        @Test
        @DisplayName("should create user")
        void shouldCreateUser() {
            // given
            final var chatId = random.nextLong();
            final var update = mockCommandUpdate("/start", chatId);
            // when
            privateChatCommandListener.processUpdate(update, bot);
            // then
            final var userInfo = userInfoRepository.findByTelegramChatId(chatId).orElseThrow();
            final var user = userInfo.getUser();
            assertNotNull(user);
            assertEquals(Role.Roles.USER.getRoleName(), user.getRole().getName());
            assertEquals(chatId + "@ai-counting.com", user.getLogin());
            assertEquals("username", userInfo.getTelegramUsername());
            assertEquals("firstName", userInfo.getFirstName());
            assertEquals("lastName", userInfo.getLastName());
            assertEquals("en", userInfo.getLanguage());
            assertTrue(userInfo.getIsTelegramPremium());
        }

        @Test
        @DisplayName("should create user without optional fields")
        void shouldCreateUserWithoutOptionalFields() {
            // given
            final var chatId = random.nextLong();
            final var update = mockCommandUpdate("/start", chatId);
            when(update.message().from().firstName()).thenReturn(null);
            when(update.message().from().lastName()).thenReturn(null);
            // when
            privateChatCommandListener.processUpdate(update, bot);
            // then
            final var userInfo = userInfoRepository.findByTelegramChatId(chatId).orElseThrow();
            final var user = userInfo.getUser();
            assertNotNull(user);
            assertEquals(Role.Roles.USER.getRoleName(), user.getRole().getName());
            assertEquals(chatId + "@ai-counting.com", user.getLogin());
            assertEquals("username", userInfo.getTelegramUsername());
            assertEquals("Unknown", userInfo.getFirstName());
            assertEquals("Unknown", userInfo.getLastName());
            assertEquals("en", userInfo.getLanguage());
            assertTrue(userInfo.getIsTelegramPremium());
        }

        @Test
        @DisplayName("should add additional chat info to user")
        void shouldAddAdditionalChatInfo() {
            // given
            final var chatId = random.nextLong();
            final var bio = "bio";
            final var update = mockCommandUpdate("/start", chatId);
            final var chat = mock(Chat.class);
            final var chatResponse = mock(GetChatResponse.class);
            when(chatResponse.chat()).thenReturn(chat);
            when(chat.bio()).thenReturn(bio);
            when(bot.execute(Mockito.any(GetChat.class))).thenReturn(chatResponse);

            // when
            privateChatCommandListener.processUpdate(update, bot);
            // then
            final var userInfo = userInfoRepository.findByTelegramChatId(chatId).orElseThrow();
            assertEquals(bio, userInfo.getBio());
        }

        @Test
        @DisplayName("works if user already exists")
        void worksIfUserExists() {
            // given
            final var chatId = random.nextLong();
            final var user = userTestDataCreator.givenUserExists(it -> {
            });
            userTestDataCreator.givenUserInfoExists(it -> {
                it.setUser(user);
                it.setTelegramChatId(chatId);
            });
            final var update = mockCommandUpdate("/start", chatId);
            // when
            privateChatCommandListener.processUpdate(update, bot);
            // then
            final var userInfo = userInfoRepository.findByTelegramChatId(chatId).orElseThrow();
            assertEquals(user, userInfo.getUser());
        }

        @Test
        @DisplayName("should send message with reply keyboard")
        void shouldSendMessageWithReplyKeyboard() {
            // given
            final var chatId = random.nextLong();
            final var update = mockCommandUpdate("/start", chatId);
            // when
            privateChatCommandListener.processUpdate(update, bot);
            // then
            assertSentMessageToChatIdContainsText(bot, chatId, "Hello firstName");
            assertSentMessageToChatIdContainsKeyboard(bot, chatId);
        }
    }

    @TestComponent
    @Nested
    @DisplayName("on pin command")
    class OnPinCommand {

        @Autowired
        private UserSettingsRepository userSettingsRepository;

        @Test
        @DisplayName("should send message with pin code request")
        void shouldSendMessageWithPinCodeRequest() {
            // given
            final var chatId = random.nextLong();
            userTestDataCreator.givenUserInfoExists(it -> it.setTelegramChatId(chatId));
            final var update = mockCommandUpdate("/pin", chatId);
            // when
            privateChatCommandListener.processUpdate(update, bot);
            // then
            assertSentMessageContainsText(bot, "Pin code is used to additionally protect your financial data. Store it in a safe place!");
        }

        @Test
        @DisplayName("should send validation if pin code is not 6 characters long")
        void shouldSendValidationIfTooShort() {
            // given
            final var chatId = random.nextLong();
            userTestDataCreator.givenUserInfoExists(it -> it.setTelegramChatId(chatId));
            final var update = mockCommandUpdate("/pin 1234", chatId);
            // when
            privateChatCommandListener.processUpdate(update, bot);
            // then
            assertSentMessageContainsText(bot, "Pin code must be 6 characters long");
        }

        @Test
        @DisplayName("should send validation if pin code contains non-numeric characters")
        void shouldSendValidationIfContainsNonNumeric() {
            // given
            final var chatId = random.nextLong();
            userTestDataCreator.givenUserInfoExists(it -> it.setTelegramChatId(chatId));
            final var update = mockCommandUpdate("/pin 1234a6", chatId);
            // when
            privateChatCommandListener.processUpdate(update, bot);
            // then
            assertSentMessageContainsText(bot, "Pin code must contain only numbers");
        }

        @Test
        @DisplayName("should save pin code")
        void shouldSavePinCode() {
            // given
            final var chatId = random.nextLong();
            final var user = userTestDataCreator.givenUserInfoExists(it -> it.setTelegramChatId(chatId)).getUser();
            final var userSettings = userTestDataCreator.givenUserSettingsExists(it -> it.setUser(user));
            final var update = mockCommandUpdate("/pin 123456", chatId);
            // when
            privateChatCommandListener.processUpdate(update, bot);
            // then
            final var settings = userSettingsRepository.findById(userSettings.getId()).orElseThrow();
            assertNotEquals(userSettings.getPinCode(), settings.getPinCode());
            assertEquals("123456", settings.getPinCode());
        }

        @Test
        @DisplayName("should create user settings if not exists")
        void shouldCreateUserSettings() {
            // given
            final var chatId = random.nextLong();
            final var user = userTestDataCreator.givenUserInfoExists(it -> it.setTelegramChatId(chatId)).getUser();
            final var update = mockCommandUpdate("/pin 123456", chatId);
            // when
            privateChatCommandListener.processUpdate(update, bot);
            // then
            final var settings = userSettingsRepository.findOneByUser(user);
            assertTrue(settings.isPresent());
            assertEquals("123456", settings.get().getPinCode());
        }
    }

    @Nested
    @DisplayName("on help command")
    class OnHelpCommand {

        @Test
        @DisplayName("should send message with help info")
        void shouldSendMessageWithHelpInfo() {
            // given
            final var chatId = random.nextLong();
            userTestDataCreator.givenUserInfoExists(it -> it.setTelegramChatId(chatId));
            final var update = mockCommandUpdate("/help", chatId);
            // when
            privateChatCommandListener.processUpdate(update, bot);
            // then
            assertSentMessageToChatIdContainsText(bot, chatId, "feedback");
        }
    }
}