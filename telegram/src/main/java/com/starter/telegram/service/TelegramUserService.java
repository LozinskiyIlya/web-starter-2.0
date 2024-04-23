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
    public User createUserIfNotExists(Update update) {
        final var chatId = update.message().chat().id();
        log.info("Creating user with chatId: {}", chatId);
        final var userInfo = userInfoRepository.findByTelegramChatId(chatId).orElseGet(() -> {
            final var created = User.randomPasswordUser(chatId + "@ai-counting.com");
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
            return userInfoRepository.save(ui);
        });
        return userInfo.getUser();
    }
}
