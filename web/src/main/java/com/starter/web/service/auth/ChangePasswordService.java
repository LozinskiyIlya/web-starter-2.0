package com.starter.web.service.auth;

import com.starter.domain.entity.User;
import com.starter.domain.repository.UserRepository;
import com.starter.web.controller.GlobalExceptionHandler.UserNotFoundException;
import com.starter.web.controller.auth.ChangePasswordController.ChangePasswordDTO;
import com.starter.web.service.user.CurrentUserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChangePasswordService {

    private final PasswordEncoder encoder;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public void requestPasswordChange(String username) {
        final var userOpt = userRepository.findByLogin(username);
        if (userOpt.isEmpty()) {
            return;
        }
        final var user = userOpt.get();
        log.info("Requesting password change for user {} unique key {}", user, UUID.randomUUID());
    }

    @Transactional
    public void changePasswordByCode(UUID code, ChangePasswordDTO dto) {
        log.info("Changing password by code {} to {}", code, dto.getNewPassword());
        // todo find user by code and change password
    }

    @Transactional
    public void changePassword(ChangePasswordDTO dto) {
        final var user = currentUserService.getUser().orElseThrow(UserNotFoundException::new);
        user.setPassword(encoder.encode(dto.getNewPassword()));
        user.setUserType(User.UserType.REAL);
        userRepository.save(user);
    }
}
