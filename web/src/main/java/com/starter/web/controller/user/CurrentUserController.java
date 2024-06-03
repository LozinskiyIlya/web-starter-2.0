package com.starter.web.controller.user;

import com.starter.domain.entity.User;
import com.starter.domain.entity.UserSettings;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.domain.repository.UserRepository;
import com.starter.common.aspect.logging.LogApiAction;
import com.starter.common.service.CurrentUserService;
import com.starter.domain.repository.UserSettingsRepository;
import com.starter.web.dto.UserSettingsDto;
import com.starter.web.mapper.UserSettingsMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/user/current")
@Schema(title = "Текущий пользователь")
@LogApiAction
public class CurrentUserController {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final UserSettingsMapper userSettingsMapper;

    @GetMapping("")
    @Operation(summary = "Отобразить данные пользователя")
    public CurrentUserDto getCurrentUser() {
        UserDetails userDetails = currentUserService.getUserDetails();
        User current = userRepository.findByLogin(userDetails.getUsername()).orElseThrow();
        CurrentUserDto dto = new CurrentUserDto();
        userInfoRepository.findOneByUser(current)
                .ifPresent(currentUserInfo -> {
                    dto.setFirstName(currentUserInfo.getFirstName());
                    dto.setLastName(currentUserInfo.getLastName());
                    final var telegramUser = new TelegramUserDto();
                    telegramUser.setId(currentUserInfo.getTelegramChatId());
                    telegramUser.setUsername(currentUserInfo.getTelegramUsername());
                    dto.setTelegramUser(telegramUser);
                });
        userSettingsRepository.findOneByUser(current)
                .map(userSettingsMapper::toDto)
                .ifPresent(dto::setSettings);
        dto.setAccountNonExpired(userDetails.isAccountNonExpired());
        dto.setAccountNonLocked(userDetails.isAccountNonLocked());
        dto.setCredentialsNonExpired(userDetails.isCredentialsNonExpired());
        dto.setEnabled(userDetails.isEnabled());
        dto.setUsername(userDetails.getUsername());
        dto.setRole(current.getRole().getName());
        dto.setUserType(current.getUserType());
        dto.setUserId(current.getId());
        return dto;
    }

    @Data
    public static class CurrentUserDto {
        private UUID userId;
        private String username;
        private String firstName;
        private String lastName;
        private String role;
        private User.UserType userType;
        private boolean isAccountNonExpired;
        private boolean isAccountNonLocked;
        private boolean isCredentialsNonExpired;
        private boolean isEnabled;
        private TelegramUserDto telegramUser;
        private UserSettingsDto settings;
    }

    @Data
    public static class TelegramUserDto {
        private Long id;
        private String username;
    }
}
