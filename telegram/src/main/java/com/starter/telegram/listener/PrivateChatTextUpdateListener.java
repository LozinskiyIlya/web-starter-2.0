package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.starter.domain.entity.Group;
import com.starter.domain.repository.GroupRepository;
import com.starter.telegram.service.TelegramUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PrivateChatTextUpdateListener extends AbstractChatUpdateListener {
    public PrivateChatTextUpdateListener(
            TelegramUserService telegramUserService,
            GroupRepository groupRepository,
            ApplicationEventPublisher publisher,
            @Qualifier("downloadDirectory") String downloadDirectory) {
        super(telegramUserService, groupRepository, publisher, downloadDirectory);
    }

    @Override
    protected Group getGroup(Update update, TelegramBot bot) {
        // here we are in private messages, find a personal "group" with same chatId
        // as user's chatId or create new "group" if not found
        final var chatId = update.message().from().id();
        return groupRepository.findByChatId(chatId).orElseGet(() -> {
            final var personal = Group.personal(chatId);
            final var owner = telegramUserService.createOrFindUser(update.message().from(), bot);
            personal.setOwner(owner);
            personal.setMembers(List.of(owner));
            return groupRepository.save(personal);
        });
    }

    @Override
    protected void checkIdMigration(Update update) {
        // never happens in private messages - do nothing
    }
}
