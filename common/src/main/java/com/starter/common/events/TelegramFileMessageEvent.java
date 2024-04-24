package com.starter.common.events;

import com.starter.domain.entity.Group;


public class TelegramFileMessageEvent extends AbstractEvent<TelegramFileMessageEvent.TelegramFileMessagePayload> {
    public TelegramFileMessageEvent(Object source, TelegramFileMessageEvent.TelegramFileMessagePayload payload) {
        super(source, payload);
    }

    public record TelegramFileMessagePayload(Group group, String fileUrl, String caption) {
    }
}
