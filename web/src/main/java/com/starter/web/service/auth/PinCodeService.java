package com.starter.web.service.auth;


import com.pengrad.telegrambot.TelegramBot;
import com.starter.common.exception.Exceptions;
import com.starter.common.service.CurrentUserService;
import com.starter.domain.entity.User;
import com.starter.domain.entity.UserSettings;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.starter.telegram.service.render.TelegramStaticRenderer.renderPin;

@Slf4j
@Service
@RequiredArgsConstructor
public class PinCodeService {
    public static final String PIN_SESSION_ATTR_NAME = "pinEntered";
    private final CurrentUserService currentUserService;
    private final TelegramBot bot;

    public boolean verifyPin(String pin, HttpSession httpSession) {
        return currentUserService.getUser()
                .map(User::getUserSettings)
                .map(UserSettings::getPinCode)
                .map(pin::equals)
                .map(valid -> {
                    httpSession.setAttribute(PIN_SESSION_ATTR_NAME, valid);
                    return valid;
                })
                .orElseThrow(Exceptions.UserNotFoundException::new);
    }

    public void resetPin(HttpSession httpSession) {
        httpSession.setAttribute(PIN_SESSION_ATTR_NAME, false);
        final var user = currentUserService.getUser()
                .orElseThrow(Exceptions.WrongUserException::new);
        final var chatId = user.getUserInfo().getTelegramChatId();
        bot.execute(renderPin(chatId));
    }
}
