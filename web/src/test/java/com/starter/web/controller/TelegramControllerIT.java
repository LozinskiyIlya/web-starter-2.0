package com.starter.web.controller;

import com.starter.web.AbstractSpringIntegrationTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TelegramControllerIT extends AbstractSpringIntegrationTest {

    @SneakyThrows
    @Test
    @DisplayName("verify init data")
    void verifyInitData() {
        // given
        final var initDataSafe = "";
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