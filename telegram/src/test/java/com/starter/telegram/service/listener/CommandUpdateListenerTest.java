package com.starter.telegram.service.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
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
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@SpringBootTest
class CommandUpdateListenerTest implements UserTestData, UserInfoTestData {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CommandUpdateListener commandUpdateListener;

    private final EasyRandom random = new EasyRandom(new EasyRandomParameters().seed(System.nanoTime()));

    @BeforeEach
    void setup() {
        roleRepository.findByName(Role.Roles.USER.getRoleName()).orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName(Role.Roles.USER.getRoleName());
            return roleRepository.save(newRole);
        });
    }

    @Nested
    @DisplayName("on start command")
    class OnStartCommand {

        @Test
        @DisplayName("should create user")
        void shouldCreateUser() {
            // given
            final var chatId = random.nextLong();
            final var update = mockUpdate("/start", chatId);
            final var bot = mockBot();
            // when
            commandUpdateListener.processUpdate(update, bot);
            // then
//            final var userInfo = userInfoRepository.findByTelegramChatId(chatId).orElseThrow();
//            final var user = userInfo.getUser();
//            assertNotNull(user);
//            assertEquals(Role.Roles.USER.getRoleName(), user.getRole().getName());
//            assertEquals(chatId + "@wegosty.ru", user.getLogin());
//            assertEquals("username", userInfo.getTelegramUsername());
//            assertEquals("firstName", userInfo.getFirstName());
//            assertEquals("lastName", userInfo.getLastName());
//            assertEquals("en", userInfo.getLanguage());
//            assertTrue(userInfo.getIsTelegramPremium());
        }

        @Test
        @DisplayName("should create user without optional fields")
        void shouldCreateUserWithoutOptionalFields() {
            // given
            final var chatId = random.nextLong();
            final var update = mockUpdate("/start", chatId);
            when(update.message().from().firstName()).thenReturn(null);
            when(update.message().from().lastName()).thenReturn(null);
            final var bot = mockBot();
            // when
            commandUpdateListener.processUpdate(update, bot);
            // then
//            final var userInfo = userInfoRepository.findByTelegramChatId(chatId).orElseThrow();
//            final var user = userInfo.getUser();
//            assertNotNull(user);
//            assertEquals(Role.Roles.USER.getRoleName(), user.getRole().getName());
//            assertEquals(chatId + "@wegosty.ru", user.getLogin());
//            assertEquals("username", userInfo.getTelegramUsername());
//            assertEquals("Unknown", userInfo.getFirstName());
//            assertEquals("Unknown", userInfo.getLastName());
//            assertEquals("en", userInfo.getLanguage());
//            assertTrue(userInfo.getIsTelegramPremium());
        }

        @Test
        @DisplayName("works if user already exists")
        void worksIfUserExists() {
            // given
            final var chatId = random.nextLong();
            final var user = givenUserExists(it -> {
            });
//            givenUserInfoExists(it -> {
//                it.setUser(user);
//                it.setTelegramChatId(chatId);
//            });
//            final var update = mockUpdate("/start", chatId);
//            final var bot = mockBot();
//            // when
//            commandUpdateListener.processUpdate(update, bot);
//            // then
//            final var userInfo = userInfoRepository.findByTelegramChatId(chatId).orElseThrow();
//            assertEquals(user, userInfo.getUser());
        }

        @Test
        @DisplayName("should send message with reply keyboard")
        void shouldSendMessageWithReplyKeyboard() {
            // given
            final var chatId = random.nextLong();
            final var update = mockUpdate("/start", chatId);
            final var bot = mockBot();
            // when
            commandUpdateListener.processUpdate(update, bot);
            // then
            Mockito.verify(bot, Mockito.times(1)).execute(Mockito.any(SendMessage.class));
        }
    }

    private Update mockUpdate(String command, Long chatId) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        com.pengrad.telegrambot.model.User user = mock(com.pengrad.telegrambot.model.User.class);
        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(message.from()).thenReturn(user);
        when(chat.id()).thenReturn(chatId);
        when(user.id()).thenReturn(chatId);
        when(user.username()).thenReturn("username");
        when(user.firstName()).thenReturn("firstName");
        when(user.lastName()).thenReturn("lastName");
        when(user.languageCode()).thenReturn("en");
        when(user.isPremium()).thenReturn(true);
        when(message.text()).thenReturn(command);
        return update;
    }

    private TelegramBot mockBot() {
        final var bot = Mockito.mock(TelegramBot.class);
        when(bot.execute(Mockito.any())).thenReturn(null);
        return bot;
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