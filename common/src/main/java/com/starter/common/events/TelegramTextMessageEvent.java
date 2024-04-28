package com.starter.common.events;

import org.springframework.data.util.Pair;

import java.util.UUID;

public class TelegramTextMessageEvent extends AbstractEvent<Pair<UUID, String>> {
    public TelegramTextMessageEvent(Object source, Pair<UUID, String> payload) {
        super(source, payload);
    }
}
