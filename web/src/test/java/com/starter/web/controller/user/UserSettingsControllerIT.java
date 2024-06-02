package com.starter.web.controller.user;

import com.starter.domain.entity.UserSettings;
import com.starter.domain.repository.UserRepository;
import com.starter.domain.repository.UserSettingsRepository;
import com.starter.domain.repository.testdata.UserTestDataCreator;
import com.starter.web.AbstractSpringIntegrationTest;
import com.starter.web.dto.UserSettingsDto;
import com.starter.web.mapper.UserSettingsMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserSettingsControllerIT extends AbstractSpringIntegrationTest {

    @Autowired
    private UserTestDataCreator userCreator;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private UserSettingsMapper userSettingsMapper;

    @RequiredArgsConstructor
    abstract class ControllerCalled {
        protected Supplier<UserSettings> settings;
        protected Supplier<Pair<String, String>> token;
        protected Supplier<ResultMatcher> expectedGetStatus;
        protected Supplier<ResultMatcher> expectedPostStatus;

        @SneakyThrows
        @Test
        @DisplayName("is expected GET result")
        void returnsExpectedResult() {
            final var auth = token.get();
            mockMvc.perform(getRequest("")
                            .header(auth.getFirst(), auth.getSecond()))
                    .andExpect(expectedGetStatus.get());
        }

        @SneakyThrows
        @Test
        @DisplayName("is expected POST result")
        void returnsExpectedPostResult() {
            final var auth = token.get();
            final var dto = userSettingsMapper.toDto(settings.get());
            mockMvc.perform(postRequest("")
                            .header(auth.getFirst(), auth.getSecond())
                            .content(mapper.writeValueAsString(dto))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(expectedPostStatus.get());
        }
    }

    @Nested
    @DisplayName("For non-existing settings 200")
    public class ForNonExistingSettings extends ControllerCalled {
        {
            final var notPersisted = new UserSettings();
            notPersisted.setId(UUID.randomUUID());
            settings = () -> notPersisted;
            token = UserSettingsControllerIT.this::testUserAuthHeader;
            expectedGetStatus = status()::isOk;
            expectedPostStatus = status()::isOk;
        }

        @SneakyThrows
        @Test
        @DisplayName("and settings created")
        void settingsCreated() {
            final var auth = token.get();
            final var user = userRepository.findByLogin(TEST_USER).orElseThrow();
            mockMvc.perform(getRequest("")
                            .header(auth.getFirst(), auth.getSecond()))
                    .andExpect(status().isOk());
            assertTrue(userSettingsRepository.findOneByUser(user).isPresent());
        }
    }

    @Nested
    @DisplayName("As non-existing user 403")
    public class AsNonExistingUser extends ControllerCalled {
        {
            settings = userCreator::givenUserSettingsExists;
            token = UserSettingsControllerIT.this::userAuthHeaderUnchecked;
            expectedGetStatus = status()::isForbidden;
            expectedPostStatus = status()::isForbidden;
        }
    }

    @Nested
    @DisplayName("As settings owner 200")
    public class AsSettingsOwner extends ControllerCalled {
        {
            final var user = userCreator.givenUserExists();
            settings = () -> userCreator.givenUserSettingsExists(s -> {
                s.setUser(user);
                s.setPinCode("123456");
                s.setSpoilerBills(true);
                s.setAutoConfirmBills(true);
            });
            token = () -> userAuthHeader(user);
            expectedGetStatus = status()::is2xxSuccessful;
            expectedPostStatus = status()::is2xxSuccessful;
        }

        @Test
        @DisplayName("and settings mapped properly")
        void mappedProperly() throws Exception {
            final var persisted = this.settings.get();
            final var header = token.get();
            final var response = mockMvc.perform(getRequest("")
                            .header(header.getFirst(), header.getSecond()))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            final var returned = mapper.readValue(response, UserSettingsDto.class);
            assertEquals(persisted.getPinCode(), returned.getPinCode());
            assertEquals(persisted.getSpoilerBills(), returned.getSpoilerBills());
            assertEquals(persisted.getAutoConfirmBills(), returned.getAutoConfirmBills());
        }
    }

    @Override
    protected String controllerPath() {
        return "/api/user/settings";
    }
}