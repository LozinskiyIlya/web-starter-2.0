package com.starter.common.events;

import java.util.UUID;


public class TelegramFileMessageEvent extends AbstractEvent<TelegramFileMessageEvent.TelegramFileMessagePayload> {
    public TelegramFileMessageEvent(Object source, TelegramFileMessageEvent.TelegramFileMessagePayload payload) {
        super(source, payload);
    }

    public record TelegramFileMessagePayload(UUID groupId, String fileUrl, String caption) {
    }
}
