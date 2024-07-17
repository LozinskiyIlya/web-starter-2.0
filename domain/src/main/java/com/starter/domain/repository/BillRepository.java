package com.starter.domain.repository;


import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @author ilya
 * @date 08.11.2021
 */

public interface BillRepository extends Repository<Bill>, PagingAndSortingRepository<Bill, UUID> {

    @Query("select b from Bill b where b.group in (:groups) and b.status <> 'SKIPPED'")
    Page<Bill> findAllNotSkippedByGroupIn(@Param("groups") List<Group> groups, Pageable pageable);

    @Query("select b from Bill b where b.group in (:groups) and b.status <> 'SKIPPED' and b.mentionedDate >= :from and b.mentionedDate <= :to")
    Page<Bill> findAllNotSkippedByGroupInAndMentionedDateBetween(@Param("groups") List<Group> groups, @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query("select count(b) from Bill b where b.group = :group and b.status <> 'SKIPPED'")
    Long countNotSkippedByGroup(@Param("group") Group group);

    @Query("select b from Bill b where b.group = :group and b.status <> 'SKIPPED' order by b.mentionedDate desc")
    List<Bill> findFirstNotSkippedByGroupOrderByMentionedDateDesc(@Param("group") Group group, Pageable pageable);

    @Query("SELECT t.name as name, t.hexColor as hexColor, SUM(b.amount) as amount " +
            "FROM Bill b " +
            "JOIN b.tags t " +
            "WHERE b.status <> 'SKIPPED' " +
            "AND b.group in (:groups) " +
            "AND b.currency = :currency " +
            "AND b.mentionedDate >= :from AND b.mentionedDate <= :to " +
            "GROUP BY t")
    List<TagAmount> findTagAmountsByGroupInAndCurrency(
            @Param("groups") List<Group> groups,
            @Param("currency") String currency,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("SELECT b.currency FROM Bill b " +
            "WHERE b.group in (:groups) " +
            "AND b.status <> 'SKIPPED' " +
            "GROUP BY b.currency " +
            "ORDER BY COUNT(b.currency) DESC")
    List<String> findMostUsedCurrenciesByGroupIn(@Param("groups") List<Group> groups, Pageable pageable);

    default Bill findFirstNotSkippedByGroupOrderByMentionedDateDesc(Group group) {
        Pageable pageable = PageRequest.of(0, 1);
        List<Bill> bills = findFirstNotSkippedByGroupOrderByMentionedDateDesc(group, pageable);
        return bills.isEmpty() ? null : bills.get(0);
    }

    default String findMostUsedCurrencyByGroup(Group group) {
        Pageable pageable = PageRequest.of(0, 1);
        List<String> currencies = findMostUsedCurrenciesByGroupIn(List.of(group), pageable);
        return currencies.isEmpty() ? "" : currencies.get(0);
    }

    default Page<Bill> findAllNotSkippedByGroup(Group group, Pageable pageable) {
        return findAllNotSkippedByGroupIn(List.of(group), pageable);
    }

    List<Bill> findAllByGroup(Group group);

    Page<Bill> findAllByGroupAndStatusEquals(Group group, Bill.BillStatus status, Pageable pageable);

    void deleteAllByGroupAndStatus(Group group, Bill.BillStatus billStatus);

    interface TagAmount {
        String getName();

        String getHexColor();

        Double getAmount();
    }
}
