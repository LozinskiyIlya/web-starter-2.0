package com.starter.web;

import com.starter.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.starter.web.populator.SwaggerUserPopulator.SWAGGER_USER;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WebStarterApplicationTest {

    @Autowired
    private UserRepository userRepository;
    @Test
    void swaggerUserPresent() {
        assertTrue(userRepository.findByLogin(SWAGGER_USER).isPresent());
    }
}