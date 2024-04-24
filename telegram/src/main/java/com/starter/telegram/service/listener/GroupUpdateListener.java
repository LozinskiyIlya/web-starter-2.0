package com.starter.telegram.service.listener;

import java.io.InputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Document;
import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetFile;
import com.starter.common.events.TelegramFileMessageEvent;
import com.starter.common.events.TelegramFileMessageEvent.TelegramFileMessagePayload;
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
        final var text = update.message().text();
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
                return downloadFileFromUrl(downloadUrl, document.fileName());
            }
        }
        return null;
    }


    private String downloadFileFromUrl(String fileUrl, String outputFileName) {
        try {
            URL url = new URL(fileUrl);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            int responseCode = httpConn.getResponseCode();

            // Check HTTP response code first
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Opens input stream from the HTTP connection
                InputStream inputStream = httpConn.getInputStream();

                // Opens an output stream to save into file
                FileOutputStream outputStream = new FileOutputStream(outputFileName);

                int bytesRead;
                byte[] buffer = new byte[4096];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();

                System.out.println("File downloaded: " + outputFileName);
                return outputFileName;
            } else {
                System.out.println("No file to download. Server replied HTTP code: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            log.error("Error while downloading file from URL", e);
            return null;
        }
    }
}
