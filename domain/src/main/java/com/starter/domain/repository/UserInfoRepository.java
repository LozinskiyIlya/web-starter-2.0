package com.starter.domain.repository;

import com.starter.domain.entity.User;
import com.starter.domain.entity.UserInfo;
import jakarta.transaction.Transactional;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * @author ilya
 * @date 27.08.2021
 */

@Transactional
public interface UserInfoRepository extends Repository<UserInfo> {

    Optional<UserInfo> findOneByUser(User user);

    Optional<UserInfo> findOneByUser_Id(UUID userId);

    Collection<UserInfo> findAllByUser(User user);

    Optional<UserInfo> findByTelegramChatId(Long chatId);
}
