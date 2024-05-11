package com.starter.telegram.service;

import com.starter.common.events.BillConfirmedEvent;
import com.starter.domain.entity.Bill;
import com.starter.telegram.AbstractTelegramTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class TelegramBillServiceTest extends AbstractTelegramTest {

    @Autowired
    private TelegramBillService service;

    @Nested
    @DisplayName("on bill created")
    class OnBillCreated {
    }

    @Nested
    @DisplayName("on bill confirmed")
    class OnBillConfirmed {

        @Test
        @DisplayName("should change status")
        void shouldChangeStatus() {
            // given
            final var bill = billTestDataCreator.givenBillExists(b -> b.setStatus(Bill.BillStatus.SENT));
            final var event = new BillConfirmedEvent(this, bill.getId());

            // when
            service.onBillConfirmed(event);

            // then
            final var updatedBill = billTestDataCreator.billRepository().findById(bill.getId()).orElseThrow();
            assertEquals(Bill.BillStatus.CONFIRMED, updatedBill.getStatus());
        }
    }
}