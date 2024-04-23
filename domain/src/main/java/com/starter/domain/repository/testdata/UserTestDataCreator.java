package com.starter.domain.repository.testdata;


import com.starter.domain.entity.Role;
import com.starter.domain.entity.User;
import com.starter.domain.entity.UserInfo;
import com.starter.domain.repository.Repository;
import com.starter.domain.repository.RoleRepository;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class UserTestDataCreator implements UserTestData, UserInfoTestData {

    private final UserInfoRepository userInfoRepository;

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
}