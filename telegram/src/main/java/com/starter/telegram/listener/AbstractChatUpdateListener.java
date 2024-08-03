package com.starter.telegram.listener;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Document;
import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetFile;
import com.starter.common.events.TelegramFileMessageEvent;
import com.starter.common.events.TelegramTextMessageEvent;
import com.starter.common.utils.CustomFileUtils;
import com.starter.domain.entity.Group;
import com.starter.domain.repository.GroupRepository;
import com.starter.telegram.service.TelegramUserService;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public abstract class AbstractChatUpdateListener implements UpdateListener {

    protected final TelegramUserService telegramUserService;
    protected final TelegramMessageRenderer telegramMessageRenderer;
    protected final GroupRepository groupRepository;
    protected final ApplicationEventPublisher publisher;
    protected final String downloadDirectory;

    @Override
    @Transactional
    public void processUpdate(Update update, TelegramBot bot) {
        log.info("Processing chat update: {}", update);
        checkIdMigration(update);
        final var group = getGroup(update, bot);
        final var text = update.message().text();
        doBillWork(bot, update, group, text);
        telegramUserService.updateUserInfo(update.message().from(), bot);
    }

    protected abstract Group getGroup(Update update, TelegramBot bot);

    protected abstract void checkIdMigration(Update update);

    private void doBillWork(TelegramBot bot, Update update, Group group, String text) {
        final var fileUrl = extractFileFromUpdate(update, bot);
        if (fileUrl != null) {
            final var fileReceivedNotice = telegramMessageRenderer.renderFileReceivedNotice(update.message().from().id());
            final var response = bot.execute(fileReceivedNotice);
            publisher.publishEvent(
                    new TelegramFileMessageEvent(this,
                            new TelegramFileMessageEvent.TelegramFileMessagePayload(group.getId(), fileUrl, text, response.message().messageId())));
            return;
        }
        if (text != null) {
            publisher.publishEvent(new TelegramTextMessageEvent(this, Pair.of(group.getId(), text)));
            return;
        }
        log.info("No text or file found in the update: {}", update);
    }

    private String extractFileFromUpdate(Update update, TelegramBot bot) {
        if (update.message() != null && update.message().document() != null) {
            Document document = update.message().document();
            log.info("File received: file_id = {}, file_name = {}, file_size = {}", document.fileId(), document.fileName(), document.fileSize());
            GetFile request = new GetFile(document.fileId());
            File file = bot.execute(request).file();

            if (file.filePath() != null) {
                String downloadUrl = bot.getFullFilePath(file);
                // Use this URL to download the file
                log.info("Download URL: {}", downloadUrl);
                return CustomFileUtils.downloadFileFromUrl(downloadUrl, document.fileName(), downloadDirectory);
            }
        }
        return extractPhotoFromUpdate(update, bot);
    }

    private String extractPhotoFromUpdate(Update update, TelegramBot bot) {
        if (update.message() != null && update.message().photo() != null) {
            var photo = update.message().photo();
            var photoSize = photo[photo.length - 1];
            log.info("Photo received: file_id = {}, file_size = {}", photoSize.fileId(), photoSize.fileSize());
            GetFile request = new GetFile(photoSize.fileId());
            File file = bot.execute(request).file();
            if (file.filePath() != null) {
                return bot.getFullFilePath(file);
            }
        }
        return null;
    }
}
