package com.starter.telegram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication(scanBasePackages = "com.starter")
public class TelegramStarterApplication {

    @EventListener(ApplicationReadyEvent.class)
    public void init() {

    }

    public static void main(String[] args) {
        SpringApplication.run(TelegramStarterApplication.class, args);
    }
}
