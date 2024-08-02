package com.starter.common.events;

import org.springframework.data.util.Pair;

import java.util.UUID;

public class BillCreatedEvent extends AbstractEvent<Pair<UUID, Integer>> {
    public BillCreatedEvent(Object source, Pair<UUID, Integer> payload) {
        super(source, payload);
    }
}
