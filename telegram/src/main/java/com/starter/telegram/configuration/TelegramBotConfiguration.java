package com.starter.telegram.configuration;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.Cancellable;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;


@Slf4j
@Configuration
@EnableConfigurationProperties({TelegramProperties.class})
public class TelegramBotConfiguration {

    @Bean
    public TelegramBot telegramBot(TelegramProperties telegramProperties) {
        return new ConfiguredTelegramBot(telegramProperties);
    }

    public static class ConfiguredTelegramBot extends TelegramBot {

        public ConfiguredTelegramBot(TelegramProperties telegramProperties) {
            super(telegramProperties.getToken());
        }

        @Override
        public <T extends BaseRequest<T, R>, R extends BaseResponse> R execute(BaseRequest<T, R> request) {
            final var baseResult = super.execute(request);
            if (!baseResult.isOk()) {
                log.error("Telegram request failed: {}", baseResult.description());
            }
            return baseResult;
        }

        @Override
        public <T extends BaseRequest<T, R>, R extends BaseResponse> Cancellable execute(T request, Callback<T, R> callback) {
            return super.execute(request, new Callback<T, R>() {
                @Override
                public void onResponse(T t, R r) {
                    if (!r.isOk()) {
                        log.error("Telegram request failed with response: {}", r.description());
                        callback.onFailure(t, new IOException("Failed response: " + r.description()));
                    } else {
                        callback.onResponse(t, r);
                    }
                }

                @Override
                public void onFailure(T t, IOException e) {
                    log.error("Telegram request failure: {}", e.getMessage());
                    callback.onFailure(t, e);
                }
            });
        }
    }
}
