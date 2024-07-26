package com.starter.telegram.service;


import com.pengrad.telegrambot.TelegramBot;
import com.starter.domain.entity.User;
import com.starter.domain.entity.UserInfo;
import com.starter.domain.entity.UserSettings;
import com.starter.domain.repository.RoleRepository;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.domain.repository.UserRepository;
import com.starter.domain.repository.UserSettingsRepository;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.starter.domain.entity.Role.Roles.USER;

@Slf4j
@RequiredArgsConstructor
@Service
public class TelegramUserService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final ExecutorService userInfoUpdateExecutor = Executors.newFixedThreadPool(2);

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @PreDestroy
    public void destroy() {
        userInfoUpdateExecutor.shutdown();
        try {
            userInfoUpdateExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            log.error("Failed to stop executor", ex);
            Thread.currentThread().interrupt();
        }
    }

    @Transactional
    public User createOrFindUser(com.pengrad.telegrambot.model.User from, TelegramBot bot) {
        final var chatId = from.id();  // sender telegram id
        log.info("Creating user with chatId: {}", chatId);
        final var userInfo = userInfoRepository.findByTelegramChatId(chatId).orElseGet(() -> {
            final var created = User.randomPasswordTelegramUser(chatId + "@ai-counting.com");
            created.setRole(roleRepository.findByName(USER.getRoleName()).orElseThrow());
            final var user = userRepository.save(created);
            final var ui = new UserInfo();
            ui.setUser(user);
            ui.setTelegramChatId(chatId);

            collectMainInfo(from, ui);
            collectAdditionalInfo(bot, ui, chatId);

            return userInfoRepository.save(ui);
        });
        return userInfo.getUser();
    }

    @Transactional
    public UserSettings createOrFindUserSettings(Long id) {
        return userInfoRepository.findByTelegramChatId(id)
                .map(UserInfo::getUser)
                .map(user -> userSettingsRepository.findOneByUser(user)
                        .orElseGet(() -> {
                            final var settings = new UserSettings();
                            settings.setUser(user);
                            return userSettingsRepository.save(settings);
                        })).orElseThrow();
    }

    @Transactional
    public UserSettings createOrFindUserSettings(User user) {
        return userSettingsRepository.findOneByUser(user)
                .orElseGet(() -> {
                    final var settings = new UserSettings();
                    settings.setUser(user);
                    return userSettingsRepository.save(settings);
                });
    }

    public void updateUserInfo(com.pengrad.telegrambot.model.User from, TelegramBot telegramBot) {
        final var chatId = from.id();
        userInfoRepository.findByTelegramChatId(chatId)
                .ifPresent(userInfo ->
                        userInfoUpdateExecutor.submit(() -> {
                            collectMainInfo(from, userInfo);
                            collectAdditionalInfo(telegramBot, userInfo, chatId);
                        }));
    }

    public void saveUserSettings(UserSettings userSettings) {
        userSettingsRepository.save(userSettings);
    }

    private void collectMainInfo(com.pengrad.telegrambot.model.User from, UserInfo userInfo) {
        if (StringUtils.hasText(from.firstName())) {
            userInfo.setFirstName(from.firstName());
        }
        if (StringUtils.hasText(from.lastName())) {
            userInfo.setLastName(from.lastName());
        }
        userInfo.setTelegramUsername(from.username());
        userInfo.setLanguage(from.languageCode());
        userInfo.setIsTelegramPremium(from.isPremium());
    }

    private void collectAdditionalInfo(TelegramBot bot, UserInfo userInfo, Long chatId) {
        try {
            final var response = bot.execute(new com.pengrad.telegrambot.request.GetChat(chatId));
            final var chat = response.chat();
            userInfo.setBio(chat.bio());
            userInfo.setDateOfBirth(chat.birthdate() != null ? chat.birthdate().toString() : null);
            userInfo.setAvatar(chat.photo() != null ? chat.photo().toString() : null);
        } catch (Exception e) {
            log.error("Error while getting additional chat info: {} skipping", e.getMessage());
        }
    }
}
