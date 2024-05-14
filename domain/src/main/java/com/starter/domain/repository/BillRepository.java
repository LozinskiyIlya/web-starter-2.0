package com.starter.domain.repository;


import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.UUID;

/**
 * @author ilya
 * @date 08.11.2021
 */

public interface BillRepository extends Repository<Bill>, PagingAndSortingRepository<Bill, UUID> {

    List<Bill> findAllByGroup(Group group);

    Page<Bill> findAllByGroup(Group group, Pageable pageable);

    Long countByGroup(Group group);
}
