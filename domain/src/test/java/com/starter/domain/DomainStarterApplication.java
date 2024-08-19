package com.starter.domain;

import com.starter.domain.entity.Role;
import com.starter.domain.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static com.starter.domain.entity.Role.Roles.values;

@SpringBootApplication
public class DomainStarterApplication {

	public static void main(String[] args) {
		SpringApplication.run(DomainStarterApplication.class, args);
	}

	@Autowired
	private RoleRepository roleRepository;

	@PostConstruct
	public void populateRolesIfMissing() {
		for (Role.Roles role : values()) {
			roleRepository.findByName(role.getRoleName()).orElseGet(() -> {
				Role newRole = new Role();
				newRole.setName(role.getRoleName());
				return roleRepository.save(newRole);
			});
		}
	}
}
