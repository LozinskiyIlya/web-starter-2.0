package com.starter.common.events;

import com.starter.domain.entity.Bill;

public class BillCreatedEvent extends AbstractEvent<Bill> {
    public BillCreatedEvent(Object source, Bill payload) {
        super(source, payload);
    }
}
