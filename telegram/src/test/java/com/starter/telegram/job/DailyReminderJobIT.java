package com.starter.telegram.job;

import com.starter.domain.entity.JobInvocationDetails;
import com.starter.domain.repository.testdata.UserTestDataCreator;
import com.starter.telegram.AbstractTelegramTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyReminderJobIT extends AbstractTelegramTest {


    @Autowired
    private DailyReminderJob job;
    @Autowired
    private UserTestDataCreator userCreator;

    @Nested
    @DisplayName("should run")
    class ShouldRun {


        @Test
        @DisplayName("should return true when details is empty")
        void shouldReturnTrueWhenDetailsIsEmpty() {
            Optional<JobInvocationDetails> details = Optional.empty();
            assertTrue(job.shouldRun(details));
        }

        @Test
        @DisplayName("should return false when last run was less than 60 minutes ago")
        void shouldReturnFalseWhenLastRunLessThan60MinutesAgo() {
            Instant lastRun = Instant.now().minus(Duration.ofMinutes(30));
            Optional<JobInvocationDetails> details = createJobInvocationDetails(lastRun);
            assertFalse(job.shouldRun(details));
        }

        @Test
        @DisplayName("should return true when last run was exactly 60 minutes ago")
        void shouldReturnTrueWhenLastRunExactly60MinutesAgo() {
            Instant lastRun = Instant.now().minus(Duration.ofMinutes(60));
            Optional<JobInvocationDetails> details = createJobInvocationDetails(lastRun);
            assertTrue(job.shouldRun(details));
        }

        @Test
        @DisplayName("should return true when last run was more than 60 minutes ago")
        void shouldReturnTrueWhenLastRunMoreThan60MinutesAgo() {
            Instant lastRun = Instant.now().minus(Duration.ofMinutes(120));
            Optional<JobInvocationDetails> details = createJobInvocationDetails(lastRun);
            assertTrue(job.shouldRun(details));
        }

        private Optional<JobInvocationDetails> createJobInvocationDetails(Instant lastRun) {
            JobInvocationDetails details = new JobInvocationDetails();
            details.setCreatedAt(lastRun);
            return Optional.of(details);
        }
    }


    @Nested
    @DisplayName("run")
    class Run {

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
            assertSentMessageToChatIdContainsText(bot, "Good evening " + userInfo.getFirstName(), userInfo.getTelegramChatId());
        }
    }

}