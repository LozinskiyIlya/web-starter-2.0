package com.starter.telegram.service;


import com.pengrad.telegrambot.TelegramBot;
import com.starter.domain.entity.User;
import com.starter.domain.entity.UserInfo;
import com.starter.domain.entity.UserSettings;
import com.starter.domain.repository.RoleRepository;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.domain.repository.UserRepository;
import com.starter.domain.repository.UserSettingsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.starter.domain.entity.Role.Roles.USER;

@Slf4j
@RequiredArgsConstructor
@Service
public class TelegramUserService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final UserSettingsRepository userSettingsRepository;

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

            if (StringUtils.hasText(from.firstName())) {
                ui.setFirstName(from.firstName());
            }
            if (StringUtils.hasText(from.lastName())) {
                ui.setLastName(from.lastName());
            }
            ui.setTelegramUsername(from.username());
            ui.setLanguage(from.languageCode());
            ui.setIsTelegramPremium(from.isPremium());
            tryCollectAdditionalInfo(bot, ui, chatId);
            return userInfoRepository.save(ui);
        });
        return userInfo.getUser();
    }

    private void tryCollectAdditionalInfo(TelegramBot bot, UserInfo userInfo, Long chatId) {
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

    public void saveUserSettings(UserSettings userSettings) {
        userSettingsRepository.save(userSettings);
    }
}
