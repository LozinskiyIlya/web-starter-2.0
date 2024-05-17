package com.starter.domain.repository;


import com.starter.domain.entity.Group;
import com.starter.domain.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author ilya
 * @date 08.11.2021
 */

public interface GroupRepository extends Repository<Group>, PagingAndSortingRepository<Group, UUID> {
    Page<Group> findAllByOwner(User owner, Pageable pageable);

    Optional<Group> findByChatId(Long chatId);

    List<Group> findAllByOwner(User owner);

    @Query("""
            select g from Group g
            join Bill b
            on g = b.group
            where g.owner = :owner
            group by g
            order by max(b.mentionedDate) desc
            """)
    Page<Group> findGroupsByOwnerOrderByLatestBill(@Param("owner") User owner, Pageable pageable);

    @Modifying
    @Transactional
    @Query("update Group g set g.chatId = ?2 where g.chatId = ?1")
    void updateChatId(Long oldChatId, Long currentChatId);

    @Query("select count(member) from Group g join g.members member where g = :group")
    Long countMembers(@Param("group") Group group);
}
