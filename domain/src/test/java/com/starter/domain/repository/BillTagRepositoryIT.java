package com.starter.domain.repository;

import com.starter.domain.entity.BillTag;
import com.starter.domain.repository.testdata.BillTestDataCreator;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("For 'BillTag' entity")
class BillTagRepositoryIT extends AbstractRepositoryTest<BillTag> {

    @Autowired
    private BillTestDataCreator billTestDataCreator;

    @Override
    BillTag createEntity() {
        return billTestDataCreator.givenBillTagExists(b -> {
        });
    }
}