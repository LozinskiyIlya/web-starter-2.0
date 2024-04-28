package com.starter.common.events;

import java.util.UUID;

public class BillCreatedEvent extends AbstractEvent<UUID> {
    public BillCreatedEvent(Object source, UUID billId) {
        super(source, billId);
    }
}
