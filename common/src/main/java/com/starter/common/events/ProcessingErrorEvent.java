package com.starter.common.events;

import org.springframework.data.util.Pair;

public class ProcessingErrorEvent extends AbstractEvent<Pair<Long, Integer>> {
    public ProcessingErrorEvent(Object source, Pair<Long, Integer> payload) {
        super(source, payload);
    }
}
