package com.starter.web.controller;

import com.starter.common.events.BillCreatedEvent;
import com.starter.common.service.JwtProvider;
import com.starter.domain.repository.BillRepository;
import com.starter.web.controller.auth.AuthController;
import com.starter.web.service.auth.TelegramAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/telegram")
@Schema(title = "Теlegram-related requests")
public class TelegramController {

    private final TelegramAuthService authService;
    private final JwtProvider jwtProvider;
    private final ApplicationEventPublisher publisher;
    private final BillRepository billRepository;

    @Operation(summary = "Exchange telegram id to token", description = "Exchange telegram id to token")
    @PostMapping("/auth/webapp")
    public AuthController.AuthResponse login(@RequestBody @Valid WebAppAuthRequest request) {
        final var user = authService.login(request.getChatId(), request.getInitDataEncoded());
        final var token = jwtProvider.generateToken(user.getLogin());
        return new AuthController.AuthResponse(token, user.getFirstLogin());
    }

    @PostMapping("/auth/pin")
    public boolean verifyPin(@RequestBody @Valid PinAuthRequest request) {
        return authService.verifyPin(request.getChatId(), request.getPin());
    }

    @PostMapping("/bills/{billId}/confirm")
    public void confirmBill(@PathVariable UUID billId) {
        billRepository.findById(billId).ifPresent(bill -> publisher.publishEvent(new BillCreatedEvent(this, bill.getId())));
    }

    @Data
    public static class TelegramAuthRequest {
        @NotNull(message = "Telegram id is required")
        @Schema(description = "Telegram id", example = "123456")
        private Long chatId;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Schema(description = "DTO for exchanging telegram id to token")
    public static class WebAppAuthRequest extends TelegramAuthRequest {
        @NotEmpty(message = "Init data is required")
        @Schema(description = "Init data", example = "initData")
        private String initDataEncoded;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Schema(description = "DTO for verifying pin")
    public static class PinAuthRequest extends TelegramAuthRequest {
        @NotEmpty(message = "Pin is required")
        @Size(min = 6, max = 6, message = "Pin must be exactly 6 digits")
        @Pattern(regexp = "\\d{6}", message = "Pin can only contain digits")
        @Schema(description = "6-digits pin", example = "123456")
        private String pin;
    }
}
