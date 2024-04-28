package com.starter.web.controller;

import com.starter.common.service.JwtProvider;
import com.starter.web.controller.auth.AuthController;
import com.starter.web.service.auth.TelegramAuthService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/telegram")
@Schema(title = "Теlegram-related requests")
public class TelegramController {

    private final TelegramAuthService authService;
    private final JwtProvider jwtProvider;

    @SneakyThrows
    @PostMapping("/auth/webapp")
    public AuthController.AuthResponse login(@RequestBody @Valid WebAppAuthRequest request) {
        final var user = authService.login(request.getChatId(), request.getInitDataEncoded());
        final var token = jwtProvider.generateToken(user.getLogin());
        return new AuthController.AuthResponse(token, user.getFirstLogin());
    }

    @Data
    @Schema(description = "DTO for exchanging telegram id to token")
    public static class WebAppAuthRequest {
        @NotNull
        @Schema(description = "Telegram id", example = "123456")
        private Long chatId;

        @NotEmpty
        @Schema(description = "Init data", example = "initData")
        private String initDataEncoded;
    }
}
