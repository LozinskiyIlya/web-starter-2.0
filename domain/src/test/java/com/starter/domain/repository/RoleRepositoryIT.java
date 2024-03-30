package com.starter.domain.repository;

import com.starter.domain.entity.Role;
import com.starter.domain.repository.testdata.RoleTestData;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("For 'Role' entity")
class RoleRepositoryIT extends AbstractRepositoryTest<Role> implements RoleTestData {

    @Autowired
    private Repository<Role> roleRepository;

    @Override
    public Repository<Role> roleRepository() {
        return roleRepository;
    }

    @Override
    Role createEntity() {
        return givenRoleExists((r) -> {
        });
    }

}