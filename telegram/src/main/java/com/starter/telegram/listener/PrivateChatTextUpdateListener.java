package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.starter.domain.entity.Group;
import com.starter.domain.repository.GroupRepository;
import com.starter.telegram.service.TelegramUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PrivateChatTextUpdateListener extends AbstractChatUpdateListener {
    public PrivateChatTextUpdateListener(TelegramUserService telegramUserService, GroupRepository groupRepository, ApplicationEventPublisher publisher) {
        super(telegramUserService, groupRepository, publisher);
    }

    @Override
    protected Group getGroup(Update update, TelegramBot bot) {
        // here we are in private messages, find a personal "group" with same chatId
        // as user's chatId or create new "group" if not found
        final var chatId = update.message().from().id();
        return groupRepository.findByChatId(chatId).orElseGet(() -> {
            final var newGroup = new Group();
            newGroup.setChatId(chatId);
            newGroup.setTitle("Personal");
            final var owner = telegramUserService.createOrFindUser(update.message().from(), bot);
            newGroup.setOwner(owner);
            newGroup.setMembers(List.of(owner));
            return groupRepository.save(newGroup);
        });
    }

    @Override
    protected void checkIdMigration(Update update) {
        // never happens in private messages - do nothing
    }
}
