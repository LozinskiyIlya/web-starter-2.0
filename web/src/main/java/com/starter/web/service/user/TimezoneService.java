package com.starter.web.service.user;

import com.starter.domain.entity.UserSettings;
import com.starter.domain.repository.UserSettingsRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DateTimeException;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimezoneService {
    public static final String TIMEZONE_HEADER = "X-Timezone";
    private final UserSettingsRepository userSettingsRepository;

    public UserSettings updateTimezone(UserSettings userSettings, HttpServletRequest request) {
        final var received = request.getHeader(TIMEZONE_HEADER);
        final var current = userSettings.getTimezone();
        if (shouldSave(received, current)) {
            userSettings.setTimezone(received);
            return userSettingsRepository.save(userSettings);
        }
        return userSettings;
    }

    private boolean shouldSave(String received, String current) {
        try {
            final var parsed = ZoneId.of(received).getId();
            return StringUtils.hasText(parsed) && !parsed.equals(current);
        } catch (DateTimeException | NullPointerException ignored) {
            log.warn("Invalid timezone received: {}", received);
            return false;
        }
    }
}
