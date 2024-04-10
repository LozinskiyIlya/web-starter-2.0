package com.starter.domain.repository.testdata;


import com.starter.domain.entity.Bill;
import com.starter.domain.repository.Repository;

import java.time.Instant;
import java.util.function.Consumer;

public interface BillTestData {

    Repository<Bill> billRepository();

    default Bill givenBillExists(Consumer<Bill> configure) {
        var bill = new Bill();
        bill.setAmount(100.0);
        bill.setCurrency("USD");
        bill.setBuyer("buyer");
        bill.setSeller("seller");
        bill.setPurpose("purpose");
        bill.setMentionedDate(Instant.ofEpochMilli(TimeTestData.TODAY_MS));
        configure.accept(bill);
        return billRepository().saveAndFlush(bill);
    }
}
