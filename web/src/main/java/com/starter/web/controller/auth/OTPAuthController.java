package com.starter.web.controller.auth;

import com.starter.common.aspect.logging.extractor.EmailUserExtractor;
import com.starter.domain.entity.User;
import com.starter.domain.repository.UserRepository;
import com.starter.common.aspect.logging.LogApiAction;
import com.starter.common.exception.Exceptions.*;
import com.starter.web.controller.auth.AuthController.AuthRequest;
import com.starter.web.controller.auth.AuthController.AuthResponse;
import com.starter.web.service.auth.AuthService;
import com.starter.web.service.auth.OTPAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author ilya
 * @date 01.03.2023
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/otp")
@Schema(title = "One time code")
@LogApiAction(userExtractor = EmailUserExtractor.class)
public class OTPAuthController {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final OTPAuthService otpAuthService;

    @Operation(summary = "Request one time code")
    @PostMapping("/challenge")
    public void challenge(@RequestBody @Valid OTPChallengeRequest challengeRequest) {
        final var login = challengeRequest.getEmail();
        final var user = userRepository.findByLogin(login)
                .orElseGet(() -> {
                    final var newUser = User.randomPasswordUser(login);
                    return authService.register(newUser.getLogin(), newUser.getPassword());
                });
        otpAuthService.challenge(user);
    }

    @Operation(summary = "Validate code")
    @PostMapping("/validate")
    public AuthResponse validate(@RequestBody AuthRequest authRequest) {
        final var user = userRepository.findByLogin(authRequest.getEmail()).orElseThrow(UserNotFoundException::new);
        final var token = otpAuthService.validate(user, authRequest.getPassword());
        return new AuthResponse(token, user.getFirstLogin());
    }


    @Data
    @Schema(title = "Request OTP challenge")
    public static class OTPChallengeRequest {

        @Schema(title = "email", example = "email@domain.com")
        @NotBlank(message = "Email is required")
        @Email(regexp = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$", message = "Invalid email format")
        private String email;
    }
}
