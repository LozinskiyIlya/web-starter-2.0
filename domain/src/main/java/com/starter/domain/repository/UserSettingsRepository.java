package com.starter.domain.repository;

import com.starter.domain.entity.User;
import com.starter.domain.entity.UserSettings;
import jakarta.transaction.Transactional;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * @author ilya
 * @date 27.08.2021
 */

@Transactional
public interface UserSettingsRepository extends Repository<UserSettings> {

    Optional<UserSettings> findOneByUser(User user);

    Optional<UserSettings> findOneByUser_Id(UUID userId);

    Collection<UserSettings> findAllByUser(User user);

}
