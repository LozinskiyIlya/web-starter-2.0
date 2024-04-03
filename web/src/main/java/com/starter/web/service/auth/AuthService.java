package com.starter.web.service.auth;


import com.starter.domain.entity.Role;
import com.starter.domain.entity.User;
import com.starter.domain.repository.RoleRepository;
import com.starter.domain.repository.UserRepository;
import com.starter.web.controller.GlobalExceptionHandler.DuplicateEmailException;
import com.starter.web.controller.GlobalExceptionHandler.UnauthorizedException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.starter.domain.entity.Role.Roles.USER;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public User login(String login, String password) {
        final var exception = new UnauthorizedException("Invalid login or password");
        final var user = userRepository.findByLogin(login).orElseThrow(() -> exception);
        if (passwordEncoder.matches(password, user.getPassword())) {
            return user;
        } else {
            throw exception;
        }
    }

    @Transactional
    public User register(String email, String password) {
        if (existsByLogin(email)) {
            throw new DuplicateEmailException();
        }
        final var user = new User();
        user.setLogin(email);
        Role userRole = roleRepository.findByName(USER.getRoleName()).orElseThrow();
        user.setRole(userRole);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstLogin(true);
        return userRepository.saveAndFlush(user);
    }

    private boolean existsByLogin(String email) {
        return userRepository.findByLogin(email).isPresent();
    }
}

