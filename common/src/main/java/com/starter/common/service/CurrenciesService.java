package com.starter.common.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class CurrenciesService {

    private final ConcurrentMap<String, Currency> currencies = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() throws IOException {
        var resource = new ClassPathResource("currencies/currencies.json");
        Map<String, Currency> tempMap = objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {
        });
        currencies.putAll(tempMap);
    }

    public String getSymbol(String code) {
        if (currencies.containsKey(code)) {
            return currencies.get(code).getSymbol();
        }
        return code;
    }

    @Data
    private static class Currency {
        private String symbol;
        private String name;
        private String symbolNative;
        private int decimalDigits;
        private int rounding;
        private String code;
        private String namePlural;
    }
}
