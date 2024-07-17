package com.starter.web.controller.auth;

import com.starter.domain.repository.testdata.UserTestDataCreator;
import com.starter.web.AbstractSpringIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;

class SessionControllerIT extends AbstractSpringIntegrationTest {

    @Autowired
    private UserTestDataCreator userCreator;


    @Nested
    @DisplayName("Pin code")
    class PinCode {

        @Test
        @DisplayName("requires authentication")
        void requiresAuthentication() throws Exception {
            mockMvc.perform(getRequest("/checkPin"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should correctly set and check pin status")
        void shouldCorrectlySetAndCheckPinStatus() throws Exception {
            final var user = userCreator.givenUserExists();
            final var token = userAuthHeader(user);
            final var session = new MockHttpSession();

            // Initially, pin should not be set
            mockMvc.perform(getRequest("/checkPin")
                            .session(session)
                            .header(token.getFirst(), token.getSecond()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", is(false)));

            // Set the pin
            mockMvc.perform(postRequest("/setPin")
                            .session(session)
                            .header(token.getFirst(), token.getSecond()))
                    .andExpect(status().isOk());

            // Check pin status again, it should now be true
            mockMvc.perform(getRequest("/checkPin")
                            .session(session)
                            .header(token.getFirst(), token.getSecond()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", is(true)));

            // Reset the pin
            mockMvc.perform(postRequest("/resetPin")
                            .session(session)
                            .header(token.getFirst(), token.getSecond()))
                    .andExpect(status().isOk());

            // Check pin status again, it should now be false
            mockMvc.perform(getRequest("/checkPin")
                            .session(session)
                            .header(token.getFirst(), token.getSecond()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", is(false)));
        }
    }

    @Override
    public String controllerPath() {
        return "/api/session";
    }
}