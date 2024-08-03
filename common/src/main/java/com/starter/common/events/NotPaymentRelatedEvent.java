package com.starter.common.events;

import org.springframework.data.util.Pair;

public class NotPaymentRelatedEvent extends AbstractEvent<Pair<Long, Integer>> {
    public NotPaymentRelatedEvent(Object source, Pair<Long, Integer> payload) {
        super(source, payload);
    }
}
