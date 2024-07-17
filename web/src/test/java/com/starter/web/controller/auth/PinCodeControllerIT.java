package com.starter.web.controller.auth;

import com.starter.domain.repository.testdata.UserTestDataCreator;
import com.starter.web.AbstractSpringIntegrationTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.starter.web.service.auth.PinCodeService.PIN_SESSION_ATTR_NAME;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PinCodeControllerIT extends AbstractSpringIntegrationTest {

    @Autowired
    private UserTestDataCreator userCreator;

    @Nested
    @DisplayName("Verify")
    class Verify {

        @Test
        @DisplayName("requires authentication")
        void requiresAuthentication() throws Exception {
            mockMvc.perform(postRequest(""))
                    .andExpect(status().isForbidden());
        }

        @SneakyThrows
        @Test
        @DisplayName("requires pin in body")
        void requiresPin() {
            final var user = userCreator.givenUserExists();
            final var token = userAuthHeader(user);
            final var pinAuthRequest = pinAuthRequest("");
            mockMvc.perform(postRequest("")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(APPLICATION_JSON)
                            .content(mapper.writeValueAsString(pinAuthRequest)))
                    .andExpect(status().isBadRequest());
        }

        @SneakyThrows
        @Test
        @DisplayName("returns 400 if invalid pin")
        void returns4xxIfInvalidPin() {
            final var pinCode = "123456";
            final var settings = userCreator.givenUserSettingsExists(s->{
                s.setPinCode(pinCode);
                s.setPinCodeEnabled(true);
            });
            final var token = userAuthHeader(settings.getUser());
            var pinAuthRequest = pinAuthRequest(pinCode + "a"); // should be digits only
            mockMvc.perform(postRequest("")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(APPLICATION_JSON)
                            .content(mapper.writeValueAsString(pinAuthRequest)))
                    .andExpect(status().isBadRequest());
            
            pinAuthRequest(pinCode + "123"); // should exactly 6 digits
            mockMvc.perform(postRequest("")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(APPLICATION_JSON)
                            .content(mapper.writeValueAsString(pinAuthRequest)))
                    .andExpect(status().isBadRequest());
        }

        @SneakyThrows
        @Test
        @DisplayName("returns false if pin is not equal")
        void returnsFalseIfPinIsNotEqual() {
            final var settings = userCreator.givenUserSettingsExists(us -> us.setPinCode("123456"));
            final var pinAuthRequest = pinAuthRequest("654321");
            final var token = userAuthHeader(settings.getUser());
            final var session = mockMvc.perform(postRequest("")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(APPLICATION_JSON)
                            .content(mapper.writeValueAsString(pinAuthRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", is(false)))
                    .andReturn().getRequest().getSession();
            assertFalse((boolean) session.getAttribute(PIN_SESSION_ATTR_NAME));
        }

        @SneakyThrows
        @Test
        @DisplayName("returns true if pin is equal")
        void returnsTrueIfPinIsEqual() {
            final var pinCode = "123456";
            final var settings = userCreator.givenUserSettingsExists(us -> us.setPinCode(pinCode));
            final var pinAuthRequest = pinAuthRequest(pinCode);
            final var token = userAuthHeader(settings.getUser());
            final var session = mockMvc.perform(postRequest("")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(APPLICATION_JSON)
                            .content(mapper.writeValueAsString(pinAuthRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", is(true)))
                    .andReturn().getRequest().getSession();
            assertTrue((boolean) session.getAttribute(PIN_SESSION_ATTR_NAME));
        }


        private static PinCodeController.PinAuthRequest pinAuthRequest(String pin) {
            final var request = new PinCodeController.PinAuthRequest();
            request.setPin(pin);
            return request;
        }
    }

    @Nested
    @DisplayName("reset pin code")
    class ResetPin {

        @SneakyThrows
        @Test
        @DisplayName("returns 403 without token")
        void returns4xxIfEmpty() {
            mockMvc.perform(postRequest("/auth/pin/reset"))
                    .andExpect(status().isForbidden());
        }

        @SneakyThrows
        @Test
        @DisplayName("works with token")
        void returnsFalseIfPinIsNotEqual() {
            final var userInfo = userCreator.givenUserInfoExists();
            final var token = userAuthHeader(userInfo.getUser());
            final var session = mockMvc.perform(postRequest("/reset")
                            .header(token.getFirst(), token.getSecond()))
                    .andExpect(status().isOk()).andReturn().getRequest().getSession();
            assertFalse((boolean) session.getAttribute(PIN_SESSION_ATTR_NAME));;
        }
    }

    @Override
    public String controllerPath() {
        return "/api/auth/pin";
    }
}