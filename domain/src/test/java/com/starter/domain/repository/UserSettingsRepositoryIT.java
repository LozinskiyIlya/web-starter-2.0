package com.starter.domain.repository;

import com.starter.domain.entity.UserSettings;
import com.starter.domain.repository.testdata.UserTestDataCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("For 'UserSettings' entity")
class UserSettingsRepositoryIT extends AbstractRepositoryTest<UserSettings> {

    @Autowired
    private UserTestDataCreator userTestDataCreator;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;


    @Override
    UserSettings createEntity() {
        return userTestDataCreator.givenUserSettingsExists(us -> {
        });
    }

    @Test
    @DisplayName("User info is mapped to user")
    void userInfoIsMappedToUser() {
        final var user = template.execute(t -> userTestDataCreator.givenUserExists());
        final var userSettings = template.execute(t -> userTestDataCreator.givenUserSettingsExists(us -> {
            us.setUser(user);
        }));
        template.executeWithoutResult(t -> {
            final var fetched = userRepository.findById(user.getId()).orElseThrow();
            assertNotNull(fetched.getUserSettings());
        });
        template.executeWithoutResult(t -> {
            final var fetched = userSettingsRepository.findById(userSettings.getId()).orElseThrow();
            assertEquals(userSettings, fetched);
            assertEquals(user, fetched.getUser());
            assertEquals(userSettings, fetched.getUser().getUserSettings());
        });
        template.executeWithoutResult(t -> {
            final var fetched = userRepository.findById(user.getId()).orElseThrow();
            assertEquals(user, fetched);
            assertEquals(userSettings, fetched.getUserSettings());
            assertEquals(user, fetched.getUserSettings().getUser());
        });
    }

}