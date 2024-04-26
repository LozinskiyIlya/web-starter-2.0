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
import java.util.Base64;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/telegram")
@Schema(title = "Теlegram-related requests")
public class TelegramController {

    private final TelegramProperties telegramProperties;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @PostMapping("/webapp/auth")
    public void verifyInitData(@RequestParam("initData") String initDataEncoded) {
        // Decode initData
        String initDataJson = new String(Base64.getDecoder().decode(initDataEncoded));
        Map<String, String> initData = objectMapper.readValue(initDataJson, new TypeReference<>() {
        });

        // Extract hash and remove it from data
        String hash = initData.get("hash");
        initData.remove("hash");

        // Create data check string
        SortedMap<String, String> sortedData = new TreeMap<>();
        initData.keySet().forEach(key -> sortedData.put(key, initData.get(key)));
        String dataCheckString = sortedData.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        // Generate HMAC
        SecretKeySpec secretKey = new SecretKeySpec(telegramProperties.getToken().getBytes(), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKey);
        byte[] hmacBytes = mac.doFinal(dataCheckString.getBytes());
        // Verify hash
        String expectedHash = Hex.encodeHexString(hmacBytes);
        if (!expectedHash.equals(hash)) {
            throw new Exceptions.UnauthorizedException("");
        }
    }

}
