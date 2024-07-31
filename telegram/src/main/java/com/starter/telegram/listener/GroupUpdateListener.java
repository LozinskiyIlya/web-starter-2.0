package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.starter.domain.entity.Group;
import com.starter.domain.repository.GroupRepository;
import com.starter.telegram.configuration.TelegramProperties;
import com.starter.telegram.service.TelegramUserService;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class GroupUpdateListener extends AbstractChatUpdateListener {

    private final TelegramProperties telegramProperties;

    public GroupUpdateListener(
            GroupRepository groupRepository,
            TelegramMessageRenderer telegramMessageRenderer,
            ApplicationEventPublisher publisher,
            TelegramUserService telegramUserService,
            TelegramProperties telegramProperties,
            @Qualifier("downloadDirectory") String downloadDirectory) {
        super(telegramUserService, telegramMessageRenderer, groupRepository, publisher, downloadDirectory);
        this.telegramProperties = telegramProperties;
    }

    @Override
    public void checkIdMigration(Update update) {
        final var currentChatId = update.message().chat().id();
        final var oldChatId = update.message().migrateFromChatId();
        if (oldChatId != null) {
            groupRepository.updateChatId(oldChatId, currentChatId);
        }
    }

    @Override
    public Group getGroup(Update update, TelegramBot bot) {
        final var groupId = update.message().chat().id();
        final var hasNewChatMembers = update.message().newChatMembers() != null;
        final var weAreTheNewMember = hasNewChatMembers && Arrays.stream(update.message().newChatMembers())
                .map(com.pengrad.telegrambot.model.User::username)
                .anyMatch(telegramProperties.getUsername()::equals);
        final var group = groupRepository.findByChatId(groupId);
        if (group.isEmpty()) {
            if (weAreTheNewMember) {
                final var newGroup = new Group();
                newGroup.setChatId(groupId);
                newGroup.setTitle(update.message().chat().title());
                // if user has not yet interacted with our bot and has just added it to the group
                final var owner = telegramUserService.createOrFindUser(update.message().from(), bot);
                newGroup.setOwner(owner);
                newGroup.setMembers(List.of(owner));
                return groupRepository.save(newGroup);
            }
        }
        return group.orElseThrow();
    }
}
