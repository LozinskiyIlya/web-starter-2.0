package com.starter.telegram.service;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.UserSettings;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.GroupRepository;
import com.starter.domain.repository.UserSettingsRepository;
import com.starter.telegram.listener.query.CallbackExecutor;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramStatsService implements CallbackExecutor {

    public final static String STATS_CALLBACK_QUERY_PREFIX = "stats_";

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
    public void execute(TelegramBot bot, CallbackQuery query, Long chatId) {

    }

    @Override
    public String getPrefix() {
        return STATS_CALLBACK_QUERY_PREFIX;
    }

    public void sendStats(TelegramBot bot, ChronoUnit timeUnit, Long chatId) {
        switch (timeUnit) {
            case MONTHS -> onThisMonth(bot, chatId);
            default -> {
            }
        }
    }

    public void onThisMonth(TelegramBot bot, Long chatId) {
        //find personal group with the same as user's chatId
        final var personal = groupRepository.findByChatId(chatId).orElseThrow();
        final var userSettings = userSettingsRepository.findOneByUser(personal.getOwner());
        final var timezone = ZoneId.of(userSettings.map(UserSettings::getTimezone).orElse("UTC"));
        final var currentMonth = getCurrentMonthForUserLocalDate(timezone);
        final var totals = billRepository.findAllNotSkippedByGroupInAndMentionedDateBetween(
                        List.of(personal),
                        currentMonth.getFirst(),
                        currentMonth.getSecond(),
                        Pageable.unpaged()
                ).stream()
                .collect(Collectors.groupingBy(Bill::getCurrency, Collectors.summingDouble(Bill::getAmount)));
        if (totals.isEmpty()) {
            final var timeRange = getMonthName(currentMonth.getFirst(), timezone);
            final var message = renderer.renderNoBills(chatId, timeRange, ChronoUnit.MONTHS);
            bot.execute(message);
            return;
        }
    }

    private Pair<Instant, Instant> getCurrentMonthForUserLocalDate(ZoneId zone) {
        final var localTime = ZonedDateTime.now(zone);
        final var startOfMonth = localTime.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        final var endOfMonth = localTime.withDayOfMonth(localTime.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
        return Pair.of(startOfMonth.toInstant(), endOfMonth.toInstant());
    }

    private static String getMonthName(Instant instant, ZoneId zone) {
        final var zonedDateTime = ZonedDateTime.ofInstant(instant, zone);
        return zonedDateTime.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
    }
}
