package com.starter.domain.repository.testdata;


import com.starter.domain.entity.UserSettings;
import com.starter.domain.repository.Repository;

import java.util.function.Consumer;

public interface UserSettingsTestData {

    Repository<UserSettings> userSettingsRepository();

    default UserSettings givenUserSettingsExists(Consumer<UserSettings> configure) {
        var userSettings = new UserSettings();
        configure.accept(userSettings);
        return userSettingsRepository().saveAndFlush(userSettings);
    }
}
