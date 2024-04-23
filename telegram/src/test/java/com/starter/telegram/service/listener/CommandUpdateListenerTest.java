package com.starter.telegram.service.listener;

import com.pengrad.telegrambot.request.SendMessage;
import com.starter.domain.entity.Role;
import com.starter.domain.entity.User;
import com.starter.domain.entity.UserInfo;
import com.starter.domain.repository.Repository;
import com.starter.domain.repository.RoleRepository;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.domain.repository.UserRepository;
import com.starter.domain.repository.testdata.UserInfoTestData;
import com.starter.domain.repository.testdata.UserTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;


class CommandUpdateListenerTest extends AbstractUpdateListenerTest implements UserTestData, UserInfoTestData {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CommandUpdateListener commandUpdateListener;

    @Nested
    @DisplayName("on start command")
    class OnStartCommand {

        @Test
        @DisplayName("should create user")
        void shouldCreateUser() {
            // given
            final var chatId = random.nextLong();
            final var update = mockCommandUpdate("/start", chatId);
            final var bot = mockBot();
            // when
            commandUpdateListener.processUpdate(update, bot);
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
            final var bot = mockBot();
            // when
            commandUpdateListener.processUpdate(update, bot);
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
        @DisplayName("works if user already exists")
        void worksIfUserExists() {
            // given
            final var chatId = random.nextLong();
            final var user = givenUserExists(it -> {
            });
            givenUserInfoExists(it -> {
                it.setUser(user);
                it.setTelegramChatId(chatId);
            });
            final var update = mockCommandUpdate("/start", chatId);
            final var bot = mockBot();
            // when
            commandUpdateListener.processUpdate(update, bot);
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
            final var bot = mockBot();
            // when
            commandUpdateListener.processUpdate(update, bot);
            // then
            Mockito.verify(bot, Mockito.times(1)).execute(Mockito.any(SendMessage.class));
        }
    }

    @Override
    public Repository<UserInfo> userInfoRepository() {
        return userInfoRepository;
    }

    @Override
    public Repository<User> userRepository() {
        return userRepository;
    }

    @Override
    public Repository<Role> roleRepository() {
        return roleRepository;
    }
}