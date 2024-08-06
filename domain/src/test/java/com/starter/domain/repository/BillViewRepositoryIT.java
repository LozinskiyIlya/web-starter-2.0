package com.starter.domain.repository;

import com.starter.domain.entity.Bill;
import com.starter.domain.entity.BillTag;
import com.starter.domain.entity.BillView;
import com.starter.domain.repository.testdata.BillTestDataCreator;
import com.starter.domain.repository.testdata.UserTestDataCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@SpringBootTest
class BillViewRepositoryIT {


    @Autowired
    private TransactionTemplate template;

    @Autowired
    private UserTestDataCreator userCreator;

    @Autowired
    private BillTestDataCreator billCreator;

    @Autowired
    private BillViewRepository viewRepository;

    @TestFactory
    @DisplayName("Can find after persisted")
    Stream<DynamicTest> canFindBillView() {
        var owner = userCreator.givenUserExists();
        var group = billCreator.givenGroupExists(g -> g.setOwner(owner));
        var foodTag = billCreator.givenBillTagExists(t -> {
            t.setUser(owner);
            t.setName("Food");
        });
        var otherTag = billCreator.givenBillTagExists(t -> {
            t.setUser(owner);
            t.setName("Other");
        });
        var bill = billCreator.givenBillExists(b -> {
            b.setStatus(Bill.BillStatus.CONFIRMED);
            b.setGroup(group);
            b.setPurpose("some purpose here");
            b.setTags(Set.of(foodTag, otherTag));
        });
        var expected = new BillView();
        expected.setId(bill.getId());
        expected.setPurpose(bill.getPurpose());
        expected.setTags(bill.getTags().stream().map(BillTag::getName).collect(Collectors.joining(",")));
        expected.setOwnerId(owner.getId());
        return Stream.of(
                new TestData("tag 1", "Food"),
                new TestData("tag 2", "Other"),
                new TestData("tags whole", expected.getTags()),
                new TestData("purpose middle word", "purpose"),
                new TestData("purpose whole", expected.getPurpose())
        ).map(it -> dynamicTest(it.testName, () -> {
            var found = viewRepository.searchBills(owner.getId(), it.searchString, Pageable.unpaged())
                    .stream()
                    .map(BillView::getId)
                    .anyMatch(expected.getId()::equals);
            assertTrue(found);
        }));
    }


    @Test
    @DisplayName("should return same count for view")
    void shouldReturnSameCount() {
        final var expectedCount = 10;
        template.executeWithoutResult(tx -> {
            for (int i = 0; i < expectedCount; i++) {
                billCreator.givenBillExists(b -> b.setStatus(Bill.BillStatus.CONFIRMED));
            }
            tx.flush();
            assertNotEquals(0, billCreator.billRepository().count());
            assertEquals(billCreator.billRepository().count(), viewRepository.count());
        });
    }

    private record TestData(String testName, String searchString) {
    }
}