package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.starter.domain.entity.Group;
import com.starter.domain.entity.UserInfo;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.GroupRepository;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.telegram.service.TelegramStatsService;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.starter.telegram.service.TelegramBotService.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeyboardButtonUpdateListener implements UpdateListener {

    private final TelegramMessageRenderer renderer;
    private final TelegramStatsService statsService;
    private final UserInfoRepository userInfoRepository;
    private final GroupRepository groupRepository;
    private final BillRepository billRepository;


    @Override
    @Transactional
    public void processUpdate(Update update, TelegramBot bot) {
        final var buttonPressed = update.message().text();
        final var chatId = update.message().chat().id();
        switch (buttonPressed) {
            case NEW_BILL_BUTTON -> {
                final var message = renderer.renderNewBill(chatId);
                bot.execute(message);
            }
            case LATEST_BILLS -> statsService.sendLatestBills(bot, chatId);
            case GROUPS -> onMyGroups(chatId, bot);
            case HELP -> onHelp(chatId, bot);
            default -> {
            }
        }
    }

    private void onMyGroups(Long chatId, TelegramBot bot) {
        userInfoRepository.findByTelegramChatId(chatId)
                .map(UserInfo::getUser)
                .map(groupRepository::findAllByOwner)
                .map(this::groupToBillCount)
                .map(groups -> renderer.renderGroups(chatId, groups))
                .ifPresent(bot::execute);
    }

    private List<Pair<Group, Long>> groupToBillCount(List<Group> groups) {
        return groups
                .stream()
                .map(group -> Pair.of(group, billRepository.countNotSkippedByGroup(group)))
                .collect(Collectors.toList());
    }

    private void onHelp(Long chatId, TelegramBot bot) {
        final var message = renderer.renderSettings(chatId);
        bot.execute(message);
    }
}
