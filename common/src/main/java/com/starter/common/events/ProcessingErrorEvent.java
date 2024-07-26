package com.starter.common.events;

public class ProcessingErrorEvent extends AbstractEvent<Long> {
    public ProcessingErrorEvent(Object source, long chatId) {
        super(source, chatId);
    }
}
