package com.starter.telegram.listener.query;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.starter.domain.repository.GroupRepository;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedList;

import static com.starter.telegram.listener.query.CallbackQueryUpdateListener.QUERY_SEPARATOR;
import static com.starter.telegram.service.render.TelegramStaticRenderer.renderAddMeRejectedUpdate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddmeCallbackExecutor implements CallbackExecutor {

    private static final String PREFIX = "addme_";
    public static final String ADDME_ACCEPT_PREFIX = PREFIX + "accept_";
    public static final String ADDME_REJECT_PREFIX = PREFIX + "reject_";

    private final GroupRepository groupRepository;
    private final UserInfoRepository userInfoRepository;
    private final TelegramMessageRenderer renderer;

    @Override
    public void execute(TelegramBot bot, CallbackQuery query, Long chatId) {
        final var callbackData = query.data();
        if (callbackData.startsWith(ADDME_ACCEPT_PREFIX)) {
            acceptAddMe(bot, query, chatId);
        } else if (callbackData.startsWith(ADDME_REJECT_PREFIX)) {
            rejectAddMe(bot, query, chatId);
        }
    }

    private void acceptAddMe(TelegramBot bot, CallbackQuery callbackQuery, Long chatId) {
        final var message = callbackQuery.maybeInaccessibleMessage();
        final var removedPrefix = callbackQuery.data().substring(ADDME_ACCEPT_PREFIX.length());
        final var parts = removedPrefix.split(QUERY_SEPARATOR);
        final var userChatId = Long.valueOf(parts[0]);
        final var groupChatId = Long.valueOf(parts[1]);
        final var userInfo = userInfoRepository.findByTelegramChatId(userChatId).orElseThrow();
        final var group = groupRepository.findByChatId(groupChatId).orElseThrow();
        if (!group.contains(userInfo.getUser())) {
            final var members = new LinkedList<>(group.getMembers());
            members.add(userInfo.getUser());
            group.setMembers(members);
            groupRepository.save(group);
            log.info("User {} added to group {}", userInfo, group);
        }
        final var messageUpdate = renderer.renderAddMeAcceptedUpdate(chatId, message, userInfo, group);
        bot.execute(messageUpdate);
    }

    private void rejectAddMe(TelegramBot bot, CallbackQuery callbackQuery, Long chatId) {
        final var message = callbackQuery.maybeInaccessibleMessage();
        final var messageUpdate = renderAddMeRejectedUpdate(chatId, message);
        bot.execute(messageUpdate);
    }


    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
