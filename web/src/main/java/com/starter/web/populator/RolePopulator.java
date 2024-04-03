package com.starter.web.populator;


import com.starter.domain.entity.Role;
import com.starter.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;


@Slf4j
@Component
@RequiredArgsConstructor
public class RolePopulator implements Populator {

    private final RoleRepository roleRepository;

    @Override
    public void populate() {
        Arrays.asList(Role.Roles.values()).forEach(role ->
                roleRepository.findByName(role.getRoleName()).ifPresentOrElse(r -> {
                }, () -> {
                    Role newRole = new Role();
                    newRole.setName(role.getRoleName());
                    roleRepository.save(newRole);
                }));
        roleRepository.flush();
    }
}
