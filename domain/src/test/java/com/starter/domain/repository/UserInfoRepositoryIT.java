package com.starter.domain.repository;

import com.starter.domain.entity.Role;
import com.starter.domain.entity.User;
import com.starter.domain.entity.UserInfo;
import com.starter.domain.repository.testdata.RoleTestData;
import com.starter.domain.repository.testdata.UserInfoTestData;
import com.starter.domain.repository.testdata.UserTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("For 'UserInfo' entity")
class UserInfoRepositoryIT extends AbstractRepositoryTest<UserInfo> implements RoleTestData, UserTestData, UserInfoTestData {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Override
    public Repository<UserInfo> userInfoRepository() {
        return userInfoRepository;
    }

    @Override
    public Repository<Role> roleRepository() {
        return roleRepository;
    }

    @Override
    public Repository<User> userRepository() {
        return userRepository;
    }

    @Override
    UserInfo createEntity() {
        var user = givenUserExists(u -> u.setRole(givenRoleExists((r) -> {
        })));
        return givenUserInfoExists(u -> {
            u.setUser(user);
        });
    }

    @Test
    @DisplayName("User info is mapped to user")
    void userInfoIsMappedToUser(){
        final var user = template.execute(t -> givenUserExists(u -> u.setRole(givenRoleExists((r) -> {
                    }))));
        final var userInfo = template.execute(t -> givenUserInfoExists(ui -> {
            ui.setUser(user);
        }));
        template.executeWithoutResult(t -> {
            final var fetched = userRepository.findById(user.getId()).orElseThrow();
            assertNotNull(fetched.getUserInfo());
            assertEquals(userInfo.getFullName(), fetched.getUserInfo().getFullName());
        });
        template.executeWithoutResult(t -> {
            final var fetched = userInfoRepository.findById(userInfo.getId()).orElseThrow();
            assertEquals(userInfo, fetched);
            assertEquals(user, fetched.getUser());
            assertEquals(userInfo, fetched.getUser().getUserInfo());
        });
        template.executeWithoutResult(t -> {
            final var fetched = userRepository.findById(user.getId()).orElseThrow();
            assertEquals(user, fetched);
            assertEquals(userInfo, fetched.getUserInfo());
            assertEquals(user, fetched.getUserInfo().getUser());
        });
    }

}