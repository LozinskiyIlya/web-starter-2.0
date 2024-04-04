package com.starter.web.controller.auth;

import com.starter.web.aspect.logging.LogApiAction;
import com.starter.web.aspect.logging.extractor.CodeUserExtractor;
import com.starter.web.aspect.logging.extractor.LoginUserExtractor;
import com.starter.web.service.auth.ChangePasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Schema(title = "Смена пароля")
public class ChangePasswordController {

    private final ChangePasswordService changePasswordService;

    @Operation(summary = "Запросить смену пароля")
    @PostMapping("/recovery")
    @LogApiAction(userExtractor = LoginUserExtractor.class)
    public void requestPasswordChange(@RequestParam("login") String login) {
        changePasswordService.requestPasswordChange(login);
    }

    @Operation(summary = "Сменить пароль по коду подтверждения")
    @PostMapping("/confirm-recovery")
    @LogApiAction(userExtractor = CodeUserExtractor.class)
    public void changePassword(@RequestParam("code") UUID code, @RequestBody @Valid ChangePasswordDTO dto) {
        changePasswordService.changePasswordByCode(code, dto);
    }

    @Operation(summary = "Сменить пароль текущему пользователю")
    @PostMapping("/password/change")
    @LogApiAction
    public void changePasswordDirectly(@RequestBody @Valid ChangePasswordDTO dto) {
        changePasswordService.changePassword(dto);
    }

    @Data
    @Schema(description = "Смена пароля")
    public static class ChangePasswordDTO {

        @NotNull
        @NotEmpty
        @Schema(description = "Новый пароль")
        private String newPassword;

    }

}
