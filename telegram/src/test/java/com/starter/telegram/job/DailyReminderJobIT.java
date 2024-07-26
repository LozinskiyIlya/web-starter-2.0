package com.starter.telegram.job;

import com.starter.domain.entity.JobInvocationDetails;
import com.starter.domain.repository.testdata.UserTestDataCreator;
import com.starter.telegram.AbstractTelegramTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyReminderJobIT extends AbstractTelegramTest {


    @Autowired
    private DailyReminderJob job;
    @Autowired
    private UserTestDataCreator userCreator;

    @Test
    @DisplayName("should run at the start of the hour")
    void shouldRunAtTheStartOfTheHour() {
        Optional<JobInvocationDetails> details = Optional.empty();
        final var currentMinute = LocalTime.now().getMinute();
        if (currentMinute == 0) {
            assertTrue(job.shouldRun(details));
        } else {
            assertFalse(job.shouldRun(details));
        }
    }

    @Test
    @DisplayName("user with disabled reminder is not notified")
    void userWithDisabledReminderIsNotNotified() {
        // given
        final var settings = userCreator.givenUserSettingsExists(s -> s.setDailyReminder(false));
        final var userInfo = userCreator.givenUserInfoExists(ui -> ui.setUser(settings.getUser()));
        // when
        job.run();
        // then
        assertMessageNotSentToChatId(bot, userInfo.getTelegramChatId());
    }

    @Test
    @DisplayName("user with different time preference is not notified")
    void userWithDifferentTimePreferenceIsNotNotified() {
        // given
        final var oneHourAhead = LocalTime.now().plusHours(1);
        final var settings = userCreator.givenUserSettingsExists(s -> s.setDailyReminderAt(oneHourAhead));
        final var userInfo = userCreator.givenUserInfoExists(ui -> ui.setUser(settings.getUser()));
        // when
        job.run();
        // then
        assertMessageNotSentToChatId(bot, userInfo.getTelegramChatId());
    }

    @Test
    @DisplayName("user with different timezone is not notified")
    void userWithDifferentTimezoneIsNotNotified() {
        // given
        final var serverTimezone = ZoneId.systemDefault();
        final var userTimezone = serverTimezone.getId().equals("America/New_York") ? "Europe/London" : "America/New_York";
        final var settings = userCreator.givenUserSettingsExists(s -> {
            s.setDailyReminderAt(LocalTime.now());
            s.setTimezone(userTimezone);
        });
        final var userInfo = userCreator.givenUserInfoExists(ui -> ui.setUser(settings.getUser()));
        // when
        job.run();
        // then
        assertMessageNotSentToChatId(bot, userInfo.getTelegramChatId());
    }

    @Test
    @DisplayName("user with proper timezone and time preference is notified")
    void userWithProperTimeIsNotified() {
        // given
        final var currentServerHour = LocalTime.now().truncatedTo(ChronoUnit.HOURS);
        final var serverTimezone = ZoneId.systemDefault();
        final var userTimezone = serverTimezone.getId().equals("America/New_York") ? "Europe/London" : "America/New_York";
        final var currentServerZonedDateTime = ZonedDateTime.now(serverTimezone)
                .withHour(currentServerHour.getHour())
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        final var userLocalZonedDateTime = currentServerZonedDateTime.withZoneSameInstant(ZoneId.of(userTimezone));
        final var userLocalHour = userLocalZonedDateTime.toLocalTime();
        final var settings = userCreator.givenUserSettingsExists(s -> {
            s.setDailyReminderAt(userLocalHour);
            s.setTimezone(userTimezone);
        });
        final var userInfo = userCreator.givenUserInfoExists(ui -> ui.setUser(settings.getUser()));
        // when
        job.run();
        // then
        assertSentMessageToChatIdContainsText(bot, userInfo.getTelegramChatId(), "Good evening " + userInfo.getFirstName());
    }

}