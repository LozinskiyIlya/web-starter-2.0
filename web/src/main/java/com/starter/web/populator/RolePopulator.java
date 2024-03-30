package com.starter.web.populator;


import com.starter.domain.entity.Role;
import com.starter.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.starter.domain.entity.Role.Roles.values;


@Slf4j
@Component
@RequiredArgsConstructor
public class RolePopulator implements Populator {

    private final RoleRepository roleRepository;

    @Override
    public void populate() {
        for (Role.Roles role : values()) {
            roleRepository.findByName(role.getRoleName()).orElseGet(() -> {
                Role newRole = new Role();
                newRole.setName(role.getRoleName());
                return roleRepository.save(newRole);
            });
        }
    }
}
