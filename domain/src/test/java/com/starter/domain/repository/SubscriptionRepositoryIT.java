package com.starter.domain.repository;

import com.starter.domain.entity.Role;
import com.starter.domain.entity.Subscription;
import com.starter.domain.entity.User;
import com.starter.domain.repository.testdata.RoleTestData;
import com.starter.domain.repository.testdata.SubscriptionTestData;
import com.starter.domain.repository.testdata.UserTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("For 'Subscription' entity")
class SubscriptionRepositoryIT extends AbstractRepositoryTest<Subscription> implements RoleTestData, UserTestData, SubscriptionTestData {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Override
    public Repository<Role> roleRepository() {
        return roleRepository;
    }

    @Override
    public Repository<User> userRepository() {
        return userRepository;
    }

    @Override
    public Repository<Subscription> subscriptionRepository() {
        return subscriptionRepository;
    }

    @Override
    Subscription createEntity() {
        return givenSubscriptionExists(s ->
                s.setUser(givenUserExists(u ->
                        u.setRole(givenRoleExists(r -> {
                        })))));
    }

    @Test
    @DisplayName("mapped to user")
    void mappedToUser() {
        final var subscription = createEntity();
        final var user = subscription.getUser();
        template.executeWithoutResult(t -> {
            final var fetched = subscriptionRepository.findOneByUser(user);
            assertTrue(fetched.isPresent());
            assertEquals(subscription.getCurrency(), fetched.get().getCurrency());
        });
    }

}