package com.starter.web.service.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starter.common.exception.Exceptions;
import com.starter.domain.entity.User;
import com.starter.domain.entity.UserInfo;
import com.starter.domain.entity.UserSettings;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.telegram.configuration.TelegramProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramAuthService {

    private final TelegramProperties telegramProperties;
    private final UserInfoRepository userInfoRepository;
    private final ObjectMapper objectMapper;

    public User login(Long chatId, String initDataEncoded) {
        final var user = userInfoRepository.findByTelegramChatId(chatId)
                .orElseThrow(Exceptions.UserNotFoundException::new).getUser();
        if (initDataIsValid(chatId, initDataEncoded)) {
            return user;
        }
        throw new Exceptions.UnauthorizedException("Invalid telegram credentials");
    }

    public boolean verifyPin(Long chatId, String pin) {
        return userInfoRepository.findByTelegramChatId(chatId)
                .map(UserInfo::getUser)
                .map(User::getUserSettings)
                .map(UserSettings::getPinCode)
                .map(pin::equals)
                .orElseThrow(Exceptions.UserNotFoundException::new);
    }

    private boolean initDataIsValid(Long chatId, String initDataEncoded) {
        try {
            String initDataDecoded = UriUtils.decode(initDataEncoded, StandardCharsets.UTF_8);

            // Parse the query string
            Map<String, String> params = Arrays.stream(initDataDecoded.split("&"))
                    .map(param -> param.split("="))
                    .collect(Collectors.toMap(p -> p[0], p -> UriUtils.decode(p[1], StandardCharsets.UTF_8)));

            // Check if chat id belongs to the user which is trying to log in
            if (!chatIdBelongsToUser(chatId, params.get("user"))) {
                return false;
            }
            // Extract hash and remove it from params
            String hash = params.remove("hash");

            // Create data check string
            String dataCheckString = params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("\n"));

            // Generate the secret key
            SecretKeySpec secretKey = new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
            byte[] botTokenHmac = mac.doFinal(telegramProperties.getToken().getBytes(StandardCharsets.UTF_8));

            // Compute HMAC using the bot token HMAC as the secret key
            secretKey = new SecretKeySpec(botTokenHmac, "HmacSHA256");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            String expectedHash = bytesToHex(hmacBytes);
            return hash.equals(expectedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private boolean chatIdBelongsToUser(Long actualChatId, String userJsonDecoded) {
        try {
            final var jsonUser = objectMapper.readTree(userJsonDecoded);
            final var chatId = jsonUser.get("id").asLong();
            return actualChatId.equals(chatId);
        } catch (Exception e) {
            return false;
        }
    }
}
