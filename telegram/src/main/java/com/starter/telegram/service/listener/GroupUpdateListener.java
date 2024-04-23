package com.starter.telegram.service.listener;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.starter.common.events.TelegramTextMessageEvent;
import com.starter.domain.entity.Group;
import com.starter.domain.repository.GroupRepository;
import com.starter.telegram.service.TelegramUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupUpdateListener implements UpdateListener {

    private final GroupRepository groupRepository;

    private final TelegramUserService telegramUserService;

    private final ApplicationEventPublisher publisher;

    @Override
    public void processUpdate(Update update, TelegramBot bot) {
        log.info("Processing group update: {}", update);
        final var user = telegramUserService.createUserIfNotExists(update);
        final var groupId = update.message().chat().id();
        final var groupTitle = update.message().chat().title();
        final var group = groupRepository.findByOwnerAndChatId(user, groupId)
                .orElseGet(() -> {
                    final var newGroup = new Group();
                    newGroup.setOwner(user);
                    newGroup.setChatId(groupId);
                    newGroup.setTitle(groupTitle);
                    return groupRepository.save(newGroup);
                });
        publisher.publishEvent(new TelegramTextMessageEvent(this, Pair.of(group, update.message().text())));
    }
}
