package com.starter.telegram.service.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Document;
import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetFile;
import com.starter.common.events.TelegramFileMessageEvent;
import com.starter.common.events.TelegramFileMessageEvent.TelegramFileMessagePayload;
import com.starter.common.events.TelegramTextMessageEvent;
import com.starter.common.utils.CustomFileUtils;
import com.starter.domain.entity.Group;
import com.starter.domain.repository.GroupRepository;
import com.starter.telegram.configuration.TelegramProperties;
import com.starter.telegram.service.TelegramUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupUpdateListener implements UpdateListener {

    private final GroupRepository groupRepository;
    private final ApplicationEventPublisher publisher;
    private final TelegramUserService telegramUserService;
    private final TelegramProperties telegramProperties;

    @Override
    public void processUpdate(Update update, TelegramBot bot) {
        log.info("Processing group update: {}", update);
        final var text = update.message().text();
        final var group = createOrFindGroup(update);
        doBillWork(bot, update, group, text);
    }

    private Group createOrFindGroup(Update update) {
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
                newGroup.setOwner(telegramUserService.createOrFindUser(update));
                return groupRepository.save(newGroup);
            }
        }
        return group.orElseThrow();
    }


    private void doBillWork(TelegramBot bot, Update update, Group group, String text) {
        final var fileUrl = extractFileFromUpdate(update, bot);
        if (fileUrl != null) {
            publisher.publishEvent(new TelegramFileMessageEvent(this, new TelegramFileMessagePayload(group, fileUrl, text)));
            return;
        }
        if (text != null) {
            publisher.publishEvent(new TelegramTextMessageEvent(this, Pair.of(group, text)));
            return;
        }
        log.info("No text or file found in the update: {}", update);
    }

    private String extractFileFromUpdate(Update update, TelegramBot bot) {
        if (update.message() != null && update.message().document() != null) {
            Document document = update.message().document();
            log.info("File received: file_id = {}, file_name = {}, file_size = {}", document.fileId(), document.fileName(), document.fileSize());
            GetFile request = new GetFile(document.fileId());
            File file = bot.execute(request).file(); // Assuming 'bot' is an instance of TelegramBot

            if (file.filePath() != null) {
                String downloadUrl = bot.getFullFilePath(file);
                // Use this URL to download the file
                log.info("Download URL: {}", downloadUrl);
                return CustomFileUtils.downloadFileFromUrl(downloadUrl, document.fileName());
            }
        }
        return null;
    }
}
