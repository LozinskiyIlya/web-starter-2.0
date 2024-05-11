package com.starter.common.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class CurrenciesServiceTest {

    @Autowired
    private CurrenciesService currenciesService;

    @Test
    @DisplayName("returns currency symbol by currency code")
    void getCurrencySymbol() {
        assertEquals("€", currenciesService.getCurrencySymbol("EUR"));
        assertEquals("$", currenciesService.getCurrencySymbol("USD"));
    }

    @Test
    @DisplayName("returns currency code if currency code is not found")
    void getCurrencySymbolNotFound() {
        assertEquals("INVALID_CODE", currenciesService.getCurrencySymbol("INVALID_CODE"));
    }
}