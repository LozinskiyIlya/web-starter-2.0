package com.starter.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starter.common.exception.Exceptions;
import com.starter.telegram.configuration.TelegramProperties;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/telegram")
@Schema(title = "Теlegram-related requests")
public class TelegramController {

    private final TelegramProperties telegramProperties;

    @SneakyThrows
    @PostMapping("/webapp/auth")
    public void verifyInitData(@RequestParam("initData") String initDataEncoded) {
        if (!isValid(initDataEncoded)) {
            throw new Exceptions.UnauthorizedException("Invalid init data");
        }
    }

    private boolean isValid(String initDataEncoded) {
        try {
            String initDataDecoded = UriUtils.decode(initDataEncoded, StandardCharsets.UTF_8);

            // Parse the query string
            Map<String, String> params = Arrays.stream(initDataDecoded.split("&"))
                    .map(param -> param.split("="))
                    .collect(Collectors.toMap(p -> p[0], p -> UriUtils.decode(p[1], StandardCharsets.UTF_8)));

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
}
