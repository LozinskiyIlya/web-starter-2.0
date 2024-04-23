package com.starter.telegram;

import com.starter.domain.entity.Role;
import com.starter.domain.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication(scanBasePackages = "com.starter")
public class TelegramStarterApplication {

    @Autowired
    private RoleRepository roleRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        roleRepository.findByName(Role.Roles.USER.getRoleName()).orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName(Role.Roles.USER.getRoleName());
            return roleRepository.save(newRole);
        });
    }

    public static void main(String[] args) {
        SpringApplication.run(TelegramStarterApplication.class, args);
    }
}
