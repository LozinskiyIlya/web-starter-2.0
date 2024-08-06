package com.starter.domain.repository;

import com.starter.domain.entity.Bill;
import com.starter.domain.repository.testdata.BillTestDataCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
class BillViewRepositoryIT {


    @Autowired
    private TransactionTemplate template;

    @Autowired
    private BillTestDataCreator billTestDataCreator;

    @Autowired
    private BillViewRepository viewRepository;

    @TestFactory
    @DisplayName("Can find after persisted")
    Stream<DynamicTest> canFindParticipantView() {
        return Stream.of();
    }


    @Test
    @DisplayName("should return same count for view")
    void shouldReturnSameCount() {
        final var expectedCount = 10;
        template.executeWithoutResult(tx -> {
            for (int i = 0; i < expectedCount; i++) {
                billTestDataCreator.givenBillExists(b -> b.setStatus(Bill.BillStatus.CONFIRMED));
            }
            tx.flush();
            assertNotEquals(0, billTestDataCreator.billRepository().count());
            assertEquals(billTestDataCreator.billRepository().count(), viewRepository.count());
        });
    }


    private record TestData(String testName, String searchString) {
    }
}