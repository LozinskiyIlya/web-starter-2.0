package com.starter.web.controller;

import com.starter.web.AbstractSpringIntegrationTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Base64;

import static com.starter.web.filter.SwaggerGuardingFilter.SWAGGER_UI_URL;
import static com.starter.web.populator.SwaggerUserPopulator.SWAGGER_PASSWORD;
import static com.starter.web.populator.SwaggerUserPopulator.SWAGGER_USER;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SwaggerIT extends AbstractSpringIntegrationTest {


    @Nested
    @DisplayName("With basic auth disabled")
    class WithoutBasicAuth {

        @Test
        @DisplayName("Swagger gives 403")
        @SneakyThrows
        public void enabled1() {
            mockMvc.perform(MockMvcRequestBuilders.get(SWAGGER_UI_URL))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Swagger gives no swagger page")
        @SneakyThrows
        public void contentIsOk() {
            mockMvc.perform(MockMvcRequestBuilders.get(SWAGGER_UI_URL))
                    .andExpect(content().string(not(containsString("<title>Swagger UI</title>"))));
        }

    }

    @Nested
    @DisplayName("With basic auth enabled")
    class WithBasicAuth {

        @Nested
        @DisplayName("And correct header provided")
        class AndHeaderIsCorrect {

            private final String header = "Basic " + Base64.getEncoder().encodeToString(String.format("%s:%s", SWAGGER_USER, SWAGGER_PASSWORD).getBytes());

            @Test
            @SneakyThrows
            @DisplayName("Swagger gives 200")
            public void enabled2() {
                mockMvc.perform(MockMvcRequestBuilders.get(SWAGGER_UI_URL)
                                .header("Authorization", header))
                        .andExpect(status().isOk());
            }

            @Test
            @SneakyThrows
            @DisplayName("Swagger gives correct page")
            public void contentIsOk2() {
                mockMvc.perform(MockMvcRequestBuilders.get(SWAGGER_UI_URL)
                                .header("Authorization", header))
                        .andExpect(content().string(containsString("<title>Swagger UI</title>")));
            }

        }

        @Nested
        @DisplayName("And wrong header provided")
        class AndHeaderIsIncorrect {

            private final String header = "Basic wrongheader";

            @Test
            @DisplayName("Swagger gives 403")
            @SneakyThrows
            public void cantUseSwagger() {
                mockMvc.perform(MockMvcRequestBuilders.get(SWAGGER_UI_URL)
                                .header("Authorization", header))
                        .andExpect(status().is4xxClientError());
            }

            @Test
            @DisplayName("Swagger gives no swagger page")
            @SneakyThrows
            public void contentIsOk() {
                mockMvc.perform(MockMvcRequestBuilders.get(SWAGGER_UI_URL)
                                .header("Authorization", header))
                        .andExpect(content().string(not(containsString("<title>Swagger UI</title>"))));
            }
        }
    }
}
