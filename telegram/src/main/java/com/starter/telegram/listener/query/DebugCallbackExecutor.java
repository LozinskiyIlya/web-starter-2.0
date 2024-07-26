package com.starter.telegram.listener.query;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.telegram.service.TelegramStatsService;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class DebugCallbackExecutor implements CallbackExecutor {

    private static final String PREFIX = "DEBUG_";
    public static final String DEBUG_DAILY_REMINDER_PREFIX = PREFIX + "daily_reminder_";
    public static final String DEBUG_WEEKLY_REPORT_PREFIX = PREFIX + "weekly_report_";
    public static final String DEBUG_NO_BILLS_STATS_PREFIX = PREFIX + "no_bills_stats_";

    private final UserInfoRepository userInfoRepository;
    private final TelegramMessageRenderer renderer;
    private final TelegramStatsService statsService;

    @Override
    public void execute(TelegramBot bot, CallbackQuery query, Long chatId) {
        final var callbackData = query.data();
        final var userInfo = userInfoRepository.findByTelegramChatId(chatId).orElseThrow();
        final var userSettings = userInfo.getUser().getUserSettings();
        if (callbackData.startsWith(DEBUG_DAILY_REMINDER_PREFIX)) {
            final var message = renderer.renderDailyReminder(userSettings);
            bot.execute(message);
        } else if (callbackData.startsWith(DEBUG_NO_BILLS_STATS_PREFIX)) {
            final var message = renderer.renderStats(chatId, "July", Map.of(), null);
            bot.execute(message);
        } else if (callbackData.startsWith(DEBUG_WEEKLY_REPORT_PREFIX)) {
            statsService.sendWeeklyReport(bot, userSettings);
        }
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
