package com.starter.web.controller.user;

import com.starter.domain.entity.Role;
import com.starter.domain.entity.User;
import com.starter.domain.entity.UserInfo;
import com.starter.domain.repository.*;
import com.starter.domain.repository.testdata.UserInfoTestData;
import com.starter.domain.repository.testdata.UserTestData;
import com.starter.web.AbstractSpringIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CurrentUserControllerIT extends AbstractSpringIntegrationTest implements UserTestData, UserInfoTestData {

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ApiActionRepository apiActionRepository;

    @Test
    @DisplayName("Returns current user based on token")
    void returnCurrentUser() throws Exception {
        var login = "current user login";
        var pass = "current user password";
        final var user = givenUserInfoExists(ui -> {
            ui.setUser(givenUserExists(u -> {
                u.setLogin(login);
                u.setPassword(pass);
            }));
            ui.setFirstName("current user first name");
            ui.setLastName("current user last name");
        }).getUser();
        var header = userAuthHeader(login);
        var serializedUser = readResource("responses/user/current_user.json")
                .replace("%USER_ID%", user.getId().toString());
        mockMvc.perform(getRequest("")
                        .header(header.getFirst(), header.getSecond()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(serializedUser, true));
    }

    @Test
    @DisplayName("Return 200 when userInfo is missing")
    void whenUserInfoIsMissingReturn200() throws Exception {
        var user = givenUserExists(u -> {
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
        var user = givenUserExists(u -> {
        });
        var header = userAuthHeader(user);
        mockMvc.perform(getRequest("")
                        .header(header.getFirst(), header.getSecond()))
                .andExpect(status().isOk());
        await().atMost(2, SECONDS).until(() -> !apiActionRepository.findAllByUserId(user.getId()).isEmpty());
        final var actions = apiActionRepository.findAllByUserId(user.getId());
        assertEquals(1, actions.size());
        assertEquals(user.getLogin(), actions.get(0).getUserQualifier());
        assertEquals("/api/user/current", actions.get(0).getPath());
        assertEquals("GET", actions.get(0).getMetadata().getHttpMethod());
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

    @Override
    protected String controllerPath() {
        return "/api/user/current";
    }
}