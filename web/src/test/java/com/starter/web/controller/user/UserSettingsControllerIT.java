package com.starter.web.controller.user;

import com.starter.domain.repository.UserSettingsRepository;
import com.starter.domain.repository.testdata.UserTestDataCreator;
import com.starter.web.AbstractSpringIntegrationTest;
import com.starter.web.dto.UserSettingsDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserSettingsControllerIT extends AbstractSpringIntegrationTest {

    @Autowired
    private UserTestDataCreator userCreator;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Test
    @DisplayName("Returns user settings on token")
    void returnsSettings() throws Exception {
        var settings = userCreator.givenUserSettingsExists(u -> {
            u.setPinCode("123456");
            u.setSpoilerBills(true);
            u.setAutoConfirmBills(true);
        });
        var header = userAuthHeader(settings.getUser());
        final var response = mockMvc.perform(getRequest("")
                        .header(header.getFirst(), header.getSecond()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        final var dto = mapper.readValue(response, UserSettingsDto.class);
        assertEquals(settings.getPinCode(), dto.getPinCode());
        assertEquals(settings.getSpoilerBills(), dto.getSpoilerBills());
        assertEquals(settings.getAutoConfirmBills(), dto.getAutoConfirmBills());
    }

    @Test
    @DisplayName("Creates user settings if missing")
    void createsIfMissing() throws Exception {
        var user = userCreator.givenUserExists(u -> {
        });
        var header = userAuthHeader(user);
        mockMvc.perform(getRequest("")
                        .header(header.getFirst(), header.getSecond()))
                .andExpect(status().isOk());
        assertTrue(userSettingsRepository.findOneByUser(user).isPresent());
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

    @Override
    protected String controllerPath() {
        return "/api/user/settings";
    }
}