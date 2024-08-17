package com.starter.web.controller;

import com.starter.domain.repository.ApiActionRepository;
import com.starter.domain.repository.testdata.UserTestDataCreator;
import com.starter.web.AbstractSpringIntegrationTest;
import com.starter.web.dto.SubscriptionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubscriptionControllerIT extends AbstractSpringIntegrationTest {

    @Autowired
    private UserTestDataCreator userCreator;

    @Autowired
    private ApiActionRepository apiActionRepository;

    @Test
    @DisplayName("Returns current subscription")
    void returnCurrentUser() throws Exception {
        final var subscription = userCreator.givenSubscriptionExists(s -> {
            s.setPrice(random.nextDouble());
            s.setCurrency(random.nextObject(String.class));
        });
        final var header = userAuthHeader(subscription.getUser());
        final var response = mockMvc.perform(getRequest("")
                        .header(header.getFirst(), header.getSecond()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        final var dto = mapper.readValue(response, SubscriptionDto.class);
        assertEquals(subscription.getCurrency(), dto.getCurrency());
        assertEquals(subscription.getPrice(), dto.getPrice());
        assertNotNull(dto.getCreatedAt());
        assertNotNull(dto.getEndsAt());
    }

    @Test
    @DisplayName("Returns null if not premium")
    void returnNullIfNotPremium() throws Exception {
        final var user = userCreator.givenUserExists();
        final var header = userAuthHeader(user);
        mockMvc.perform(getRequest("")
                        .header(header.getFirst(), header.getSecond()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }


    @Test
    @DisplayName("Return 403 when user is missing")
    void whenUserIsMissingReturn403() throws Exception {
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

    @Override
    protected String controllerPath() {
        return "/api/subscription";
    }
}