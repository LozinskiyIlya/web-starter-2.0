package com.starter.domain.repository.testdata;


import com.starter.domain.entity.Role;
import com.starter.domain.entity.User;
import com.starter.domain.repository.Repository;

import java.util.UUID;
import java.util.function.Consumer;

public interface UserTestData {

    Repository<User> userRepository();

    Repository<Role> roleRepository();

    default User givenUserExists(Consumer<User> configure) {
        var user = new User();
        user.setRole(roleRepository().findAll().stream().filter(it -> it.getName().contains("USER")).findFirst().orElseThrow());
        user.setLogin(UUID.randomUUID() +"@gmail.com");
        user.setPassword("b4a60a7779e66241ffe1fb9ea15ec595");
        configure.accept(user);
        return userRepository().saveAndFlush(user);
    }

    default User givenUserWithAdminRoleExists(Consumer<User> configure) {
        var user = new User();
        user.setRole(roleRepository().findAll().stream().filter(it -> it.getName().contains("ADMIN")).findFirst().orElseThrow());
        user.setLogin(UUID.randomUUID().toString());
        user.setPassword("password");
        user.setFirstLogin(false);
        configure.accept(user);
        return userRepository().saveAndFlush(user);
    }

    default User givenUserWithInternalAdminRoleExists(Consumer<User> configure) {
        var user = new User();
        user.setRole(roleRepository().findAll().stream().filter(it -> it.getName().contains("INTERNAL_ADMIN")).findFirst().orElseThrow());
        user.setLogin(UUID.randomUUID().toString());
        user.setPassword("password");
        user.setFirstLogin(false);
        configure.accept(user);
        return userRepository().saveAndFlush(user);
    }
}
