package com.starter.common.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public abstract class AbstractEvent<T> extends ApplicationEvent {

    private final T payload;

    public AbstractEvent(Object source, T payload) {
        super(source);
        this.payload = payload;
    }

}
