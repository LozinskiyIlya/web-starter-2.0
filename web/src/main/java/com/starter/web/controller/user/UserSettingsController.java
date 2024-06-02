package com.starter.web.controller.user;

import com.starter.common.service.CurrentUserService;
import com.starter.domain.entity.UserSettings;
import com.starter.domain.repository.UserSettingsRepository;
import com.starter.web.dto.UserSettingsDto;
import com.starter.web.mapper.UserSettingsMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/user/settings")
@Schema(title = "Настройки пользователя")
public class UserSettingsController {

    private final CurrentUserService currentUserService;
    private final UserSettingsRepository userSettingsRepository;
    private final UserSettingsMapper userSettingsMapper;

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
        return userSettingsMapper.toDto(settings);
    }

    @PostMapping("")
    @Operation(summary = "Изменить настройки пользователя")
    public Instant updateUserSettings(@RequestBody @Valid UserSettingsDto dto) {
        final var current = currentUserService.getUser().orElseThrow();
        var settings = userSettingsRepository.findOneByUser(current)
                .orElseGet(() -> {
                    final var newSettings = new UserSettings();
                    newSettings.setUser(current);
                    return userSettingsRepository.save(newSettings);
                });
        var updated = userSettingsMapper.updateEntityFromDto(dto, settings);
        updated = userSettingsRepository.save(settings);
        return updated.getLastUpdatedAt();
    }
}
