package com.starter.domain.repository.testdata;


import com.starter.domain.entity.BillTag;
import com.starter.domain.repository.Repository;

import java.util.UUID;
import java.util.function.Consumer;

public interface BillTagTestData {

    Repository<BillTag> billTagRepository();

    default BillTag givenBillTagExists(Consumer<BillTag> configure) {
        var billTag = new BillTag();
        billTag.setName("billTag" + UUID.randomUUID());
        billTag.setHexColor("#FF0000");
        configure.accept(billTag);
        return billTagRepository().saveAndFlush(billTag);
    }
}
