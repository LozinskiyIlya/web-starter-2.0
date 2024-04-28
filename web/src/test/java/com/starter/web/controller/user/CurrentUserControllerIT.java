package com.starter.web.controller.user;

import com.starter.domain.repository.ApiActionRepository;
import com.starter.domain.repository.testdata.UserTestDataCreator;
import com.starter.web.AbstractSpringIntegrationTest;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CurrentUserControllerIT extends AbstractSpringIntegrationTest {

    @Autowired
    private UserTestDataCreator userCreator;

    @Autowired
    private ApiActionRepository apiActionRepository;

    @Test
    @DisplayName("Returns current user based on token")
    void returnCurrentUser() throws Exception {
        var login = "current user login";
        var pass = "current user password";
        var chatId = new EasyRandom().nextObject(Long.class);
        final var user = userCreator.givenUserInfoExists(ui -> {
            ui.setUser(userCreator.givenUserExists(u -> {
                u.setLogin(login);
                u.setPassword(pass);
            }));
            ui.setFirstName("current user first name");
            ui.setLastName("current user last name");
            ui.setTelegramUsername("current user telegram username");
            ui.setTelegramChatId(chatId);
        }).getUser();
        userCreator.givenUserSettingsExists(us -> {
            us.setUser(user);
            us.setPinCode("123456");
        });
        var header = userAuthHeader(login);
        var serializedUser = readResource("responses/user/current_user.json")
                .replace("%USER_ID%", user.getId().toString())
                .replace("\"%TELEGRAM_CHAT_ID%\"", chatId.toString());
        mockMvc.perform(getRequest("")
                        .header(header.getFirst(), header.getSecond()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(serializedUser, true));
    }

    @Test
    @DisplayName("Return 200 when userInfo is missing")
    void whenUserInfoIsMissingReturn200() throws Exception {
        var user = userCreator.givenUserExists(u -> {
        });
        var header = userAuthHeader(user);
        mockMvc.perform(getRequest("")
                        .header(header.getFirst(), header.getSecond()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Return 403 when user is missing")
    void whenUserIsMissingReturn500() throws Exception {
        var header = userAuthHeaderUnchecked(UUID.randomUUID().toString());
        mockMvc.perform(getRequest("")
                        .header(header.getFirst(), header.getSecond()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Return 403 when token is missing")
    void whenTokenIsMissingReturn403() throws Exception {
        mockMvc.perform(getRequest(""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Save api action")
    void saveApiAction() throws Exception {
        var user = userCreator.givenUserExists(u -> {
        });
        var header = userAuthHeader(user);
        mockMvc.perform(getRequest("")
                        .header(header.getFirst(), header.getSecond()))
                .andExpect(status().isOk());
        await().atMost(2, SECONDS).until(() -> !apiActionRepository.findAllByUserId(user.getId()).isEmpty());
        final var actions = apiActionRepository.findAllByUserId(user.getId());
        assertEquals(1, actions.size());
        assertNull(actions.get(0).getError());
        assertEquals(user.getLogin(), actions.get(0).getUserQualifier());
        assertEquals("/api/user/current", actions.get(0).getPath());
        assertEquals("GET", actions.get(0).getMetadata().getHttpMethod());
    }

    @Override
    protected String controllerPath() {
        return "/api/user/current";
    }
}