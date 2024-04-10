package com.starter.domain.repository;

import com.starter.domain.entity.Bill;
import com.starter.domain.repository.testdata.BillTestDataCreator;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("For 'Bill' entity")
class BillRepositoryIT extends AbstractRepositoryTest<Bill> {

    @Autowired
    private BillTestDataCreator groupCreator;

    @Override
    Bill createEntity() {
        return groupCreator.givenBillExists(b -> {
        });
    }
}