package com.starter.web.controller.user;

import com.starter.common.service.CurrentUserService;
import com.starter.domain.entity.User;
import com.starter.domain.entity.UserSettings;
import com.starter.domain.repository.UserSettingsRepository;
import com.starter.web.dto.UserSettingsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/user/settings")
@Schema(title = "Настройки пользователя")
public class UserSettingsController {

    private final CurrentUserService currentUserService;
    private final UserSettingsRepository userSettingsRepository;

    @GetMapping("")
    @Operation(summary = "Отобразить настройки пользователя")
    public UserSettingsDto getUserSettings() {
        final var current = currentUserService.getUser().orElseThrow();
        final var settings = userSettingsRepository.findOneByUser(current)
                .orElseGet(() -> {
                    final var newSettings = new UserSettings();
                    newSettings.setUser(current);
                    return userSettingsRepository.save(newSettings);
                });
        final var dto = new UserSettingsDto();
        dto.setPinCode(settings.getPinCode());
        dto.setSpoilerBills(settings.getSpoilerBills());
        dto.setAutoConfirmBills(settings.getAutoConfirmBills());
        dto.setLastUpdatedAt(settings.getLastUpdatedAt().toString());
        return dto;
    }
}
