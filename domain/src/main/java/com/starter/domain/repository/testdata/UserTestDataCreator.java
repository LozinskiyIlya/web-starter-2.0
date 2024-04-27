package com.starter.domain.repository.testdata;


import com.starter.domain.entity.Role;
import com.starter.domain.entity.User;
import com.starter.domain.entity.UserInfo;
import com.starter.domain.entity.UserSettings;
import com.starter.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class UserTestDataCreator implements UserTestData, UserInfoTestData, UserSettingsTestData {

    private final UserInfoRepository userInfoRepository;
    private final UserSettingsRepository userSettingsRepository;

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    public User givenUserExists() {
        return givenUserExists(u -> {
        });
    }

    public UserInfo givenUserInfoExists(Consumer<UserInfo> configure) {
        Consumer<UserInfo> fullyConfigure = ui -> {
            ui.setUser(givenUserExists(u -> {
            }));
            configure.accept(ui);
        };
        return UserInfoTestData.super.givenUserInfoExists(fullyConfigure);
    }

    public UserSettings givenUserSettingsExists(Consumer<UserSettings> configure) {
        Consumer<UserSettings> fullyConfigure = us -> {
            us.setUser(givenUserExists(u -> {
            }));
            configure.accept(us);
        };
        return UserSettingsTestData.super.givenUserSettingsExists(fullyConfigure);
    }

    @Override
    public Repository<UserInfo> userInfoRepository() {
        return userInfoRepository;
    }

    @Override
    public Repository<User> userRepository() {
        return userRepository;
    }

    @Override
    public Repository<Role> roleRepository() {
        return roleRepository;
    }

    @Override
    public Repository<UserSettings> userSettingsRepository() {
        return userSettingsRepository;
    }
}