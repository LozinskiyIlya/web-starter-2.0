package com.starter.web;


import com.starter.web.populator.SwaggerUserPopulator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController("/api/v1/auth")
public class AuthController {

    @PostMapping("/login")
    @Operation(summary = "Получение jwt токена", description = "Login to the application and receive a JWT token")
    public String login(@Valid @RequestBody AuthRequest request) {
        return "login";
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

}
