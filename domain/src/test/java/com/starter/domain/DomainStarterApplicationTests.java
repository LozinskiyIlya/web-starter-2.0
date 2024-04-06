package com.starter.domain;

import com.starter.domain.entity.Role;
import com.starter.domain.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;


import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DomainStarterApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private RoleRepository roleRepository;

	@Test
	void contextLoads() {
		assertNotNull(context);
	}

	@Test
	void rolesPopulated() {
		for (Role.Roles role : Role.Roles.values()) {
			assertTrue(roleRepository.findByName(role.getRoleName()).isPresent());
		}
	}
}
