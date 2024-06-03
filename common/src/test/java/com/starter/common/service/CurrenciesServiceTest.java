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
        assertEquals("€", currenciesService.getSymbol("EUR"));
        assertEquals("$", currenciesService.getSymbol("USD"));
    }

    @Test
    @DisplayName("returns currency code if currency code is not found")
    void getCurrencySymbolNotFound() {
        assertEquals("INVALID_CODE", currenciesService.getSymbol("INVALID_CODE"));
    }
}