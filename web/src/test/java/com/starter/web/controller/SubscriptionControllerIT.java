package com.starter.web.controller;

import com.starter.domain.repository.ApiActionRepository;
import com.starter.domain.repository.testdata.UserTestDataCreator;
import com.starter.web.AbstractSpringIntegrationTest;
import com.starter.web.dto.SubscriptionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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
            s.setEndsAt(LocalDateTime.now().plusMonths(1).toInstant(ZoneOffset.UTC));
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
        assertTrue(dto.isActive());
    }

    @Test
    @DisplayName("Returns active:false if expired")
    void returnNotActiveIfExpired() throws Exception {
        final var subscription = userCreator.givenSubscriptionExists(s -> {
            s.setPrice(random.nextDouble());
            s.setCurrency(random.nextObject(String.class));
            s.setEndsAt(LocalDateTime.now().minusDays(1).toInstant(ZoneOffset.UTC));
        });
        final var header = userAuthHeader(subscription.getUser());
        final var response = mockMvc.perform(getRequest("")
                        .header(header.getFirst(), header.getSecond()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        final var dto = mapper.readValue(response, SubscriptionDto.class);
        assertFalse(dto.isActive());
    }

    @Test
    @DisplayName("Returns active:false if no subscriptions")
    void returnNotActiveIfNoSubscriptions() throws Exception {
        final var user = userCreator.givenUserExists();
        final var header = userAuthHeader(user);
        final var response = mockMvc.perform(getRequest("")
                        .header(header.getFirst(), header.getSecond()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        final var dto = mapper.readValue(response, SubscriptionDto.class);
        assertFalse(dto.isActive());
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