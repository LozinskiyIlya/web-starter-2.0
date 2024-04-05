package com.starter.web.service;

import com.starter.domain.entity.Role;
import com.starter.domain.entity.User;
import com.starter.domain.repository.Repository;
import com.starter.domain.repository.RoleRepository;
import com.starter.domain.repository.UserRepository;
import com.starter.domain.repository.testdata.UserTestData;
import com.starter.web.AbstractSpringIntegrationTest;
import com.starter.web.configuration.auth.OTPAuthConfig.OTPGenerator;
import com.starter.web.controller.GlobalExceptionHandler;
import com.starter.web.service.auth.OTPAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class OTPAuthServiceIT extends AbstractSpringIntegrationTest implements UserTestData {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OTPGenerator otpGenerator;

    @Autowired
    private OTPAuthService otpService;

    @Nested
    @DisplayName("Generate")
    class Generate {

        @Test
        @DisplayName("should generate otp based on email confirmation")
        void shouldGenerateOtp() {
            final var user = givenUserExists(u -> {
            });
            final var code = otpGenerator.generate(user.getId(), Instant.now());
            assertNotNull(code);
            assertEquals(otpGenerator.getPasswordLength(), code.length());
        }

        @Test
        @DisplayName("should generate same otp for same object")
        void shouldGenerateSameOtpBasedOnSameObject() {
            final var user = givenUserExists(u -> {
            });
            final var code = otpGenerator.generate(user.getId(), Instant.now());
            assertEquals(code, otpGenerator.generate(user.getId(), Instant.now()));
        }

        @Test
        @DisplayName("should generate otp for any given time")
        void shouldGenerateOtpForAnyGivenTime() {
            final var user = givenUserExists(u -> {
            });
            final var code = otpGenerator.generate(user.getId(), Instant.now().minus(Duration.ofDays(365)));
            assertNotNull(code);
            assertEquals(otpGenerator.getPasswordLength(), code.length());
        }
    }


    @Nested
    @DisplayName("Validate")
    class Validate {

        @Test
        @DisplayName("should throw InvalidOtp if codes do not match")
        void shouldThrowInvalidOtpIfCodesDoNotMatch() {
            final var user = givenUserExists(u -> {
            });
            final var invalidCode = RandomStringUtils.random(6);
            assertThrows(GlobalExceptionHandler.InvalidOtpException.class,
                    () -> otpService.validate(user, invalidCode));
        }

        @Test
        @DisplayName("should throw InvalidOtp if code has expired")
        void shouldThrowInvalidOtpIfCodeHasExpired() {
            final var user = givenUserExists(u -> {
            });
            final var longTimeAgo = Instant.now().minus(otpGenerator.getTimeStep()).minus(Duration.ofMinutes(1));
            final var expiredCode = otpGenerator.generate(user.getId(), longTimeAgo);
            assertThrows(GlobalExceptionHandler.InvalidOtpException.class,
                    () -> otpService.validate(user, expiredCode));
        }

        @Test
        @DisplayName("should throw InvalidOtp if code has just expired")
        void shouldThrowInvalidOtpIfCodeHasJustExpired() {
            final var user = givenUserExists(u -> {
            });
            final var justExpired = Instant.now().minus(otpGenerator.getTimeStep());
            final var expiredCode = otpGenerator.generate(user.getId(), justExpired);
            assertThrows(GlobalExceptionHandler.InvalidOtpException.class,
                    () -> otpService.validate(user, expiredCode));
        }

        // todo: use some entity with timestamp to make code valid for 5 full minutes, not 5 minutes window with fix start, like 00:05, 00:10, 00:15
//        @Test
//        @DisplayName("should return token if code is almost expired")
//        void shouldReturnTokenIfCodeIsAlmostExpired() {
//            final var user = givenUserExists(u -> {
//            });
//            final var almostExpired = Instant.now().minus(otpGenerator.getTimeStep()).plus(Duration.ofMinutes(2));
//            final var expiredCode = otpGenerator.generate(user.getId(), almostExpired);
//            System.out.println("code: " + expiredCode + " time: " + almostExpired.toString() + " user: " + user.getId());
//            assertNotNull(otpService.validate(user, expiredCode));
//        }

        @Test
        @DisplayName("should return token if code is valid")
        void shouldReturnTokenIfCodeIsValid() {
            final var user = givenUserExists(u -> {
            });
            final var expiredCode = otpGenerator.generate(user.getId(), Instant.now());
            assertNotNull(otpService.validate(user, expiredCode));
        }

    }


    @Override
    public Repository<User> userRepository() {
        return userRepository;
    }

    @Override
    public Repository<Role> roleRepository() {
        return roleRepository;
    }
}