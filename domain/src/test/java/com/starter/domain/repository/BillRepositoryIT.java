package com.starter.domain.repository;

import com.starter.domain.entity.Bill;
import com.starter.domain.repository.testdata.GroupTestDataCreator;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("For 'Bill' entity")
class BillRepositoryIT extends AbstractRepositoryTest<Bill> {

    @Autowired
    private GroupTestDataCreator groupCreator;

    @Override
    Bill createEntity() {
        return groupCreator.givenBillExists(b -> {
        });
    }
}