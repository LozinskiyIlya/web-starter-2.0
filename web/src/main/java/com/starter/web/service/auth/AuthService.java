package com.starter.web.service.auth;


import com.starter.domain.entity.User;
import com.starter.domain.repository.UserRepository;
import com.starter.web.controller.GlobalExceptionHandler.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
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
}

