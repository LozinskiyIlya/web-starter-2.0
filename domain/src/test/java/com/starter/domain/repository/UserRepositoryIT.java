package com.starter.domain.repository;

import com.starter.domain.entity.Role;
import com.starter.domain.entity.User;
import com.starter.domain.repository.testdata.RoleTestData;
import com.starter.domain.repository.testdata.UserTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("For 'User' entity")
class UserRepositoryIT extends AbstractRepositoryTest<User> implements RoleTestData, UserTestData {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Repository<Role> roleRepository() {
        return roleRepository;
    }

    @Override
    public Repository<User> userRepository() {
        return userRepository;
    }

    @Override
    User createEntity() {
        return givenUserExists(u -> u.setRole(
                givenRoleExists((r) -> {
                })));
    }

    @Test
    void googleUserCanBeSavedAfterPasswordChanged() {
        final var role = givenRoleExists(r -> r.setName("some mock user role"));
        final var mockUserId = template.execute(tx -> {
            final var googleUser = User.googleUser("testGoogleUser");
            googleUser.setRole(role);
            return userRepository.save(googleUser);
        }).getId();
        template.executeWithoutResult(tx -> {
            var mockUser = userRepository.findById(mockUserId).orElseThrow();
            assertNotNull(mockUser.getId());
            assertNotNull(mockUser.getPassword());
            assertNotNull(mockUser.getLogin());
            assertEquals(roleRepository.findById(role.getId()).get(), mockUser.getRole());
            assertEquals(User.UserType.GOOGLE, mockUser.getUserType());
        });
    }
}