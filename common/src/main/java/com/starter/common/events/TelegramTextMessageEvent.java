package com.starter.common.events;

import com.starter.domain.entity.Group;
import org.springframework.data.util.Pair;

public class TelegramTextMessageEvent extends AbstractEvent<Pair<Group, String>> {
    public TelegramTextMessageEvent(Object source, Pair<Group, String> payload) {
        super(source, payload);
    }
}
