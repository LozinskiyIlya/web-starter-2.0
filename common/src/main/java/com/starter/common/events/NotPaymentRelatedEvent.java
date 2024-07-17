package com.starter.common.events;

public class NotPaymentRelatedEvent extends AbstractEvent<Long> {
    public NotPaymentRelatedEvent(Object source, long chatId) {
        super(source, chatId);
    }
}
