package com.starter.web.controller;

import com.starter.domain.repository.testdata.UserTestDataCreator;
import com.starter.web.AbstractSpringIntegrationTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TelegramControllerIT extends AbstractSpringIntegrationTest {


    @Autowired
    private UserTestDataCreator userCreator;

    @Nested
    @DisplayName("login")
    class Login {

        @SneakyThrows
        @Test
        @DisplayName("returns 400 if empty")
        void returns4xxIfEmpty() {
            final var authRequest = authRequest("", 123456L);
            mockMvc.perform(postRequest("/auth/webapp")
                            .contentType(APPLICATION_JSON)
                            .content(mapper.writeValueAsString(authRequest)))
                    .andExpect(status().isBadRequest());

        }

        @SneakyThrows
        @Test
        @DisplayName("returns 401 if based on some foreign telegram token")
        void returns4xxIfBasedOnForeignToken() {
            final var notSignedInitData = "query_id=AAEdElIZAAAAAB0SUhmMLJx-&user=%7B%22id%22%3A424808989%2C%22first_name%22%3A%22Ilya%22%2C%22last_name%22%3A%22%22%2C%22username%22%3A%22ilialoz%22%2C%22language_code%22%3A%22en%22%2C%22is_premium%22%3Atrue%2C%22allows_write_to_pm%22%3Atrue%7D&auth_date=1714133271&hash=52785bed7aefd6c757762e1a08b4a213e7b359ddd458017d5a79ce0c857da042";
            final var authRequest = authRequest(notSignedInitData, 123456L);
            mockMvc.perform(postRequest("/auth/webapp")
                            .contentType(APPLICATION_JSON)
                            .content(mapper.writeValueAsString(authRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @SneakyThrows
        @Test
        @DisplayName("returns 2xx if signed with our telegram token")
        void successfulLogin() {
            // given
            final var validInitData = "user=%7B%22id%22%3A424808989%2C%22first_name%22%3A%22Ilya%22%2C%22last_name%22%3A%22%22%2C%22username%22%3A%22ilialoz%22%2C%22language_code%22%3A%22en%22%2C%22is_premium%22%3Atrue%2C%22allows_write_to_pm%22%3Atrue%7D&chat_instance=8865578296467838294&chat_type=sender&auth_date=1714134067&hash=f82a194643761180598c1d80512d07458114240983e0e3e7e2112cda86e7bc41";
            final var userInfo = userCreator.givenUserInfoExists(ui -> {
            });
            final var authRequest = authRequest(validInitData, userInfo.getTelegramChatId());
            // when and then
            mockMvc.perform(postRequest("/auth/webapp")
                            .contentType(APPLICATION_JSON)
                            .content(mapper.writeValueAsString(authRequest)))
                    .andExpect(status().is2xxSuccessful());
        }

        @SneakyThrows
        @Test
        @DisplayName("returns 404 if user not found")
        void verifyInitDataSignedProperly() {
            // given
            final var validInitData = "user=%7B%22id%22%3A424808989%2C%22first_name%22%3A%22Ilya%22%2C%22last_name%22%3A%22%22%2C%22username%22%3A%22ilialoz%22%2C%22language_code%22%3A%22en%22%2C%22is_premium%22%3Atrue%2C%22allows_write_to_pm%22%3Atrue%7D&chat_instance=8865578296467838294&chat_type=sender&auth_date=1714134067&hash=f82a194643761180598c1d80512d07458114240983e0e3e7e2112cda86e7bc41";
            final var authRequest = authRequest(validInitData, 123456L);
            // when and then
            mockMvc.perform(postRequest("/auth/webapp")
                            .contentType(APPLICATION_JSON)
                            .content(mapper.writeValueAsString(authRequest)))
                    .andExpect(status().isNotFound());
        }

        private static TelegramController.WebAppAuthRequest authRequest(String initData, Long chatId) {
            final var request = new TelegramController.WebAppAuthRequest();
            request.setChatId(chatId);
            request.setInitDataEncoded(initData);
            return request;
        }
    }

    @Override
    protected String controllerPath() {
        return "/api/telegram";
    }
}