package com.starter.domain.repository.testdata;


import com.starter.domain.entity.Role;
import com.starter.domain.repository.Repository;

import java.util.function.Consumer;

public interface RoleTestData {

    Repository<Role> roleRepository();

    default Role givenRoleExists(Consumer<Role> configure) {
        var role = new Role();
        role.setName("role");
        configure.accept(role);
        return roleRepository().saveAndFlush(role);
    }
}
