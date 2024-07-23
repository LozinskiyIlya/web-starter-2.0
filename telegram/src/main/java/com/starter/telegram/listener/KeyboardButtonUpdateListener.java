package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.starter.domain.entity.Bill;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.GroupRepository;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.starter.telegram.service.TelegramBotService.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeyboardButtonUpdateListener implements UpdateListener {

    private final TelegramMessageRenderer renderer;
    private final GroupRepository groupRepository;
    private final BillRepository billRepository;

    @Override
    public void processUpdate(Update update, TelegramBot bot) {
        final var buttonPressed = update.message().text();
        final var chatId = update.message().chat().id();
        switch (buttonPressed) {
            case NEW_BILL_BUTTON -> {
                final var message = renderer.renderNewBill(chatId);
                bot.execute(message);
            }
            case THIS_MONTH -> onThisMonth(chatId, bot);
            case GROUPS -> onMyGroups(chatId, bot);
            case HELP -> onHelp(chatId, bot);
            default -> {
            }
        }
    }

    private void onThisMonth(Long chatId, TelegramBot bot) {
        //find personal group with the same as user's chatId
        final var personal = groupRepository.findByChatId(chatId).orElseThrow();
        final var timezone = ZoneId.of(personal.getOwner().getUserSettings().getTimezone());
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
            final var message = renderer.renderNoBills(chatId, timeRange);
            bot.execute(message);
            return;
        }

    }

    private void onMyGroups(Long chatId, TelegramBot bot) {
        final var message = renderer.renderSettings(chatId);
        bot.execute(message);
    }

    private void onHelp(Long chatId, TelegramBot bot) {
        final var message = renderer.renderSettings(chatId);
        bot.execute(message);
    }

    private Pair<Instant, Instant> getCurrentMonthForUserLocalDate(ZoneId zone) {
        final var localTime = ZonedDateTime.now(zone);
        final var startOfMonth = localTime.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        final var endOfMonth = localTime.withDayOfMonth(localTime.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
        return Pair.of(startOfMonth.toInstant(), endOfMonth.toInstant());
    }

    public static String getMonthName(Instant instant, ZoneId zone) {
        final var zonedDateTime = ZonedDateTime.ofInstant(instant, zone);
        return zonedDateTime.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
    }
}
