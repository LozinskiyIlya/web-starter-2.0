package com.starter.web.controller.auth;


import com.starter.web.populator.SwaggerUserPopulator;
import com.starter.web.service.auth.AuthService;
import com.starter.web.service.auth.JwtProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/auth")
@Schema(description = "Контроллер для авторизации")
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    @PostMapping("/login")
    @Operation(summary = "Получение jwt токена", description = "Login to the application and receive a JWT token")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        final var user = authService.login(request.getLogin(), request.getPassword());
        final var token = jwtProvider.generateToken(user.getLogin());
        return new AuthResponse(token, user.getFirstLogin());
    }

    @PostMapping("/register")
    public AuthResponse registerUser(@RequestBody @Valid RegistrationRequest request) {
        final var user = authService.register(request.getEmail(), request.getPassword());
        final var token = jwtProvider.generateToken(user.getLogin());
        return new AuthResponse(token, user.getFirstLogin());
    }


    @Data
    @Schema(description = "DTO для получение jwt токена")
    public static class AuthRequest {

        @NotNull
        @NotEmpty
        @Schema(description = "логин пользователя", example = SwaggerUserPopulator.SWAGGER_USER)
        private String login;

        @NotNull
        @NotEmpty
        @Schema(description = "пароль пользователя", example = SwaggerUserPopulator.SWAGGER_PASSWORD)
        private String password;
    }

    @Data
    @Schema(description = "DTO для получение регистрации пользователя")
    public static class RegistrationRequest {

        @NotBlank(message = "Email is required")
        @Email(regexp = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$", message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password must be at least 8 chars")
        @Pattern(regexp = "|.{8,128}")
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {

        @Schema(description = "JWT")
        private String token;

        @Schema(description = "Can be used for triggering actions, required on user creation")
        private Boolean firstLogin;
    }
}
