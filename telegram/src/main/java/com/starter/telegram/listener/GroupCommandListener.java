package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.starter.domain.repository.GroupRepository;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.telegram.service.TelegramUserService;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class GroupCommandListener extends AbstractCommandListener {

    private final GroupRepository groupRepository;
    private final UserInfoRepository userInfoRepository;
    private final TelegramUserService telegramUserService;
    private final TelegramMessageRenderer renderer;

    @Override
    public void processUpdate(Update update, TelegramBot bot) {
        final var commandParts = parseCommand(update.message().text());
        switch (commandParts.getFirst()) {
            case "/addme" -> onAddMeCommand(update, bot);
            default -> onUnknownCommand(update, bot, commandParts.getFirst());
        }
    }

    private void onAddMeCommand(Update update, TelegramBot bot) {
        final var from = update.message().from();
        final var chatId = update.message().chat().id();
        final var requestingPermissionUser = telegramUserService.createOrFindUser(from, bot);
        final var requestingPermission = userInfoRepository.findOneByUser(requestingPermissionUser).orElseThrow();
        final var group = groupRepository.findByChatId(chatId).orElseThrow();
        final var owner = userInfoRepository.findOneByUser(group.getOwner()).orElseThrow();
        final var request = renderer.renderAddMeMessage(owner, requestingPermission, group);
        bot.execute(request);
    }
}
