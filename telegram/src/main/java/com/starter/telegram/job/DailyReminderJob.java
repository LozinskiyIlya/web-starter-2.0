package com.starter.telegram.job;

import com.pengrad.telegrambot.TelegramBot;
import com.starter.common.job.Job;
import com.starter.domain.entity.JobInvocationDetails;
import com.starter.domain.repository.UserSettingsRepository;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class DailyReminderJob implements Job {

    private final TelegramBot bot;
    private final TelegramMessageRenderer messageRenderer;
    private final UserSettingsRepository userSettingsRepository;

    @Override
    public boolean shouldRun(Optional<JobInvocationDetails> details) {
        // run once per hour at the start of the hour
        final var now = Instant.now();
        return details
                .map(JobInvocationDetails::getCreatedAt)
                .map(lastRun -> Duration.between(lastRun, now).toMinutes())
                .map(minutesSinceLastRun -> minutesSinceLastRun >= 59)
                .orElse(LocalTime.ofInstant(now, ZoneId.systemDefault()).getMinute() <= 1);
    }

    @Override
    public void run() {
        userSettingsRepository.findAllByDailyReminder(true)
                .stream()
                .filter(settings -> {
                    final var userLocalTime = LocalTime.now(ZoneId.of(settings.getTimezone()));
                    return userLocalTime.getHour() == settings.getDailyReminderAt().getHour();
                })
                .map(messageRenderer::renderDailyReminder)
                .forEach(bot::execute);
    }
}
