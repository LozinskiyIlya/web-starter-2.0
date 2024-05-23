package com.starter.domain.repository;


import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * @author ilya
 * @date 08.11.2021
 */

public interface BillRepository extends Repository<Bill>, PagingAndSortingRepository<Bill, UUID> {

    @Query("select b from Bill b where b.group = :group and b.status <> 'SKIPPED'")
    Page<Bill> findAllNotSkippedByGroup(@Param("group") Group group, Pageable pageable);

    @Query("select count(b) from Bill b where b.group = :group and b.status <> 'SKIPPED'")
    Long countNotSkippedByGroup(@Param("group") Group group);

    @Query("select b from Bill b where b.group = :group and b.status <> 'SKIPPED' order by b.mentionedDate desc")
    List<Bill> findFirstNotSkippedByGroupOrderByMentionedDateDesc(@Param("group") Group group, Pageable pageable);

    @Query("SELECT t.name as name, t.hexColor as hexColor, SUM(b.amount) as amount " +
            "FROM Bill b " +
            "JOIN b.tags t " +
            "WHERE b.status <> 'SKIPPED' " +
            "AND b.group = :group " +
            "AND b.currency = :currency " +
            "GROUP BY t")
    List<TagAmount> findTagAmountsByGroupAndCurrency(@Param("group") Group group, @Param("currency") String currency);

    @Query("SELECT b.currency FROM Bill b " +
            "WHERE b.group = :group " +
            "GROUP BY b.currency " +
            "ORDER BY COUNT(b.currency) DESC")
    List<String> findMostUsedCurrencyByGroup(@Param("group") Group group, Pageable pageable);

    default Bill findFirstNotSkippedByGroupOrderByMentionedDateDesc(Group group) {
        Pageable pageable = PageRequest.of(0, 1);
        List<Bill> bills = findFirstNotSkippedByGroupOrderByMentionedDateDesc(group, pageable);
        return bills.isEmpty() ? null : bills.get(0);
    }

    default String findMostUsedCurrencyByGroup(Group group) {
        Pageable pageable = PageRequest.of(0, 1);
        List<String> currencies = findMostUsedCurrencyByGroup(group, pageable);
        return currencies.isEmpty() ? "" : currencies.get(0);
    }

    List<Bill> findAllByGroup(Group group);

    interface TagAmount {
        String getName();

        String getHexColor();

        Double getAmount();
    }
}
