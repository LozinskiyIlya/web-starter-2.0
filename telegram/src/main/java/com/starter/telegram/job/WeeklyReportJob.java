package com.starter.telegram.job;

import com.pengrad.telegrambot.TelegramBot;
import com.starter.common.job.Job;
import com.starter.domain.entity.JobInvocationDetails;
import com.starter.domain.repository.UserSettingsRepository;
import com.starter.telegram.service.TelegramStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyReportJob implements Job {

    private final TelegramBot bot;
    private final TelegramStatsService statsService;
    private final UserSettingsRepository userSettingsRepository;

    @Override
    public boolean shouldRun(Optional<JobInvocationDetails> details) {
        // run once per hour at the start of the hour
        return LocalTime.now().getMinute() <= 1;
    }

    @Override
    public void run() {
        userSettingsRepository.findAllByWeeklyReport(true)
                .stream()
                .filter(settings -> {
                    final var userLocalTime = LocalDateTime.now(ZoneId.of(settings.getTimezone()));
                    final var userLocalDayOfWeek = userLocalTime.getDayOfWeek();
                    return userLocalDayOfWeek.equals(DayOfWeek.SUNDAY) &&
                            userLocalTime.getHour() == 12 &&
                            userLocalTime.getMinute() == 0;
                })
                .forEach(settings -> {
                    try {
                        statsService.sendWeeklyReport(bot, settings);
                    } catch (Exception e) {
                        log.error("Failed to send weekly report to user {}", settings.getUser().getId(), e);
                    }
                });
    }
}
