package com.starter.common.events;

import java.util.UUID;

public class BillConfirmedEvent extends AbstractEvent<UUID> {
    public BillConfirmedEvent(Object source, UUID billId) {
        super(source, billId);
    }
}