package com.starter.telegram.service;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.message.MaybeInaccessibleMessage;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Bill_;
import com.starter.domain.entity.UserSettings;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.GroupRepository;
import com.starter.domain.repository.UserSettingsRepository;
import com.starter.telegram.listener.query.CallbackExecutor;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramStatsService implements CallbackExecutor {

    public final static String STATS_CALLBACK_QUERY_PREFIX = "stats_";
    private final static int LATEST_BILLS_COUNT = 5;
    private final GroupRepository groupRepository;
    private final BillRepository billRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final TelegramMessageRenderer renderer;

    public static final LinkedHashMap<ChronoUnit, String> AVAILABLE_UNITS = new LinkedHashMap<>();

    static {
        AVAILABLE_UNITS.put(ChronoUnit.DAYS, "Today");
        AVAILABLE_UNITS.put(ChronoUnit.WEEKS, "This week");
        AVAILABLE_UNITS.put(ChronoUnit.MONTHS, "This month");
    }

    @Override
    public String getPrefix() {
        return STATS_CALLBACK_QUERY_PREFIX;
    }

    @Override
    public void execute(TelegramBot bot, CallbackQuery query, Long chatId) {
        final var callbackData = query.data();
        final var timeUnit = ChronoUnit.valueOf(callbackData.replace(STATS_CALLBACK_QUERY_PREFIX, ""));
        sendStats(bot, timeUnit, chatId, query.maybeInaccessibleMessage());
    }

    public void sendLatestBills(TelegramBot bot, Long chatId) {
        final var personal = groupRepository.findByChatId(chatId).orElseThrow();
        final var pageRequest = PageRequest.of(0, LATEST_BILLS_COUNT, Sort.Direction.DESC, Bill_.MENTIONED_DATE);
        final var lastBills = billRepository.findAllNotSkippedByGroup(personal, pageRequest);
        final var message = renderer.renderLatestBills(chatId, lastBills, personal.getTitle());
        bot.execute(message);
    }

    private void sendStats(TelegramBot bot, ChronoUnit timeUnit, Long chatId, MaybeInaccessibleMessage previousMessage) {
        //find personal group with the same as user's chatId
        final var personal = groupRepository.findByChatId(chatId).orElseThrow();
        final var userSettings = userSettingsRepository.findOneByUser(personal.getOwner());
        final var timezone = ZoneId.of(userSettings.map(UserSettings::getTimezone).orElse("UTC"));
        final var timeRange = getZonedTimeRange(timeUnit, timezone);
        final var totals = billRepository.findAllNotSkippedByGroupInAndMentionedDateBetween(
                        List.of(personal),
                        timeRange.getFirst(),
                        timeRange.getSecond(),
                        Pageable.unpaged()
                ).stream()
                .collect(Collectors.groupingBy(Bill::getCurrency, Collectors.summingDouble(Bill::getAmount)));
        final var timeRangeText = getTimeRangeDisplayText(timeRange, timezone, timeUnit);
        final var message = renderer.renderStats(chatId, timeRangeText, totals, previousMessage);
        bot.execute(message);
    }

    private static Pair<Instant, Instant> getZonedTimeRange(ChronoUnit unit, ZoneId zone) {
        final ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime start;
        ZonedDateTime end = switch (unit) {
            case DAYS -> {
                start = now.truncatedTo(ChronoUnit.DAYS);
                yield start.plusDays(1);
            }
            case WEEKS -> {
                start = now.with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);
                yield start.plusWeeks(1);
            }
            case MONTHS -> {
                start = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                yield start.plusMonths(1);
            }
            default -> throw new IllegalArgumentException("Unsupported Chrono Unit: " + unit);
        };

        return Pair.of(start.toInstant(), end.toInstant());
    }

    private static String getTimeRangeDisplayText(Pair<Instant, Instant> range, ZoneId zone, ChronoUnit timeUnit) {
        ZonedDateTime startDate = ZonedDateTime.ofInstant(range.getFirst(), zone);
        ZonedDateTime endDate = ZonedDateTime.ofInstant(range.getSecond(), zone);
        return switch (timeUnit) {
            case DAYS -> {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E dd"); // Mon 27
                yield "Today, " + startDate.format(formatter);
            }
            case WEEKS -> {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd"); // Jul 20 - Jul 27
                yield startDate.format(formatter) + " - " + endDate.format(formatter);
            }
            case MONTHS -> {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy"); // Jul 2024
                yield startDate.format(formatter);
            }
            default -> throw new IllegalArgumentException("Unsupported time unit: " + timeUnit);
        };
    }
}
