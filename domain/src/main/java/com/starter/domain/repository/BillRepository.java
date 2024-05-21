package com.starter.domain.repository;


import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.UUID;

/**
 * @author ilya
 * @date 08.11.2021
 */

public interface BillRepository extends Repository<Bill>, PagingAndSortingRepository<Bill, UUID> {

    @Query("select b from Bill b where b.group = :group and b.status <> 'SKIPPED'")
    Page<Bill> findAllNotSkippedByGroup(Group group, Pageable pageable);

    @Query("select b from Bill b where b.group = :group and b.status <> 'SKIPPED' order by b.mentionedDate desc")
    Bill findFirstNotSkippedByGroupOrderByMentionedDateDesc(Group group);

    @Query("select count(b) from Bill b where b.group = :group and b.status <> 'SKIPPED'")
    Long countNotSkippedByGroup(Group group);

    List<Bill> findAllByGroup(Group group);
}
