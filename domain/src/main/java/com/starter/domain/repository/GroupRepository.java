package com.starter.domain.repository;


import com.starter.domain.entity.Group;
import com.starter.domain.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * @author ilya
 * @date 08.11.2021
 */

public interface GroupRepository extends Repository<Group> {

    List<Group> findAllByOwner(User owner);

    Optional<Group> findByChatId(Long chatId);

    @Modifying
    @Transactional
    @Query("update Group g set g.chatId = ?2 where g.chatId = ?1")
    void updateChatId(Long oldChatId, Long currentChatId);
}
