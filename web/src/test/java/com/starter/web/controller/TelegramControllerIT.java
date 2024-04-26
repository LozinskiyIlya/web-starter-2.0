package com.starter.web.controller;

import com.starter.web.AbstractSpringIntegrationTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TelegramControllerIT extends AbstractSpringIntegrationTest {

    @SneakyThrows
    @Test
    @DisplayName("returns 4xx if empty")
    void returns4xxIfEmpty() {
        mockMvc.perform(postRequest("/webapp/auth")
                        .param("initData", ""))
                .andExpect(status().is4xxClientError());

    }

    @SneakyThrows
    @Test
    @DisplayName("returns 4xx if based on some foreign telegram token")
    void returns4xxIfBasedOnForeignToken() {
        final var initData = "query_id=AAEdElIZAAAAAB0SUhmMLJx-&user=%7B%22id%22%3A424808989%2C%22first_name%22%3A%22Ilya%22%2C%22last_name%22%3A%22%22%2C%22username%22%3A%22ilialoz%22%2C%22language_code%22%3A%22en%22%2C%22is_premium%22%3Atrue%2C%22allows_write_to_pm%22%3Atrue%7D&auth_date=1714133271&hash=52785bed7aefd6c757762e1a08b4a213e7b359ddd458017d5a79ce0c857da042";
        mockMvc.perform(postRequest("/webapp/auth")
                        .param("initData", initData))
                .andExpect(status().is4xxClientError());

    }

    @SneakyThrows
    @Test
    @DisplayName("verify init data signed with our telegram token")
    void verifyInitDataSignedProperly() {
        // given
        final var initDataSafe = "user=%7B%22id%22%3A424808989%2C%22first_name%22%3A%22Ilya%22%2C%22last_name%22%3A%22%22%2C%22username%22%3A%22ilialoz%22%2C%22language_code%22%3A%22en%22%2C%22is_premium%22%3Atrue%2C%22allows_write_to_pm%22%3Atrue%7D&chat_instance=8865578296467838294&chat_type=sender&auth_date=1714134067&hash=f82a194643761180598c1d80512d07458114240983e0e3e7e2112cda86e7bc41";
        // when and then
        mockMvc.perform(postRequest("/webapp/auth")
                        .param("initData", initDataSafe))
                .andExpect(status().isOk());

    }

    @Override
    protected String controllerPath() {
        return "/api/telegram";
    }
}