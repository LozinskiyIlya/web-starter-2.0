package com.starter.telegram.service;


import com.pengrad.telegrambot.model.Update;
import com.starter.domain.entity.User;
import com.starter.domain.entity.UserInfo;
import com.starter.domain.repository.RoleRepository;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.domain.repository.UserRepository;
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


    @Transactional
    public User createOrFindUser(Update update) {
        final var chat = update.message().chat(); // can be a group;
        final var chatId = update.message().from().id(); // sender telegram id
        log.info("Creating user with chatId: {}", chatId);
        final var userInfo = userInfoRepository.findByTelegramChatId(chatId).orElseGet(() -> {
            final var created = User.randomPasswordTelegramUser(chatId + "@ai-counting.com");
            created.setRole(roleRepository.findByName(USER.getRoleName()).orElseThrow());
            final var user = userRepository.save(created);
            final var ui = new UserInfo();
            ui.setUser(user);
            ui.setTelegramChatId(chatId);
            final var from = update.message().from();
            if (StringUtils.hasText(from.firstName())) {
                ui.setFirstName(from.firstName());
            }
            if (StringUtils.hasText(from.lastName())) {
                ui.setLastName(from.lastName());
            }
            ui.setTelegramUsername(from.username());
            ui.setLanguage(from.languageCode());
            ui.setIsTelegramPremium(from.isPremium());
            ui.setBio(chat.bio());
            ui.setDateOfBirth(chat.birthdate() != null ? chat.birthdate().toString() : null);
            ui.setAvatar(chat.photo() != null ? chat.photo().toString() : null);
            return userInfoRepository.save(ui);
        });
        return userInfo.getUser();
    }
}
