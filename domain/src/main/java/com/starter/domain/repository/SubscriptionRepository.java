package com.starter.domain.repository;

import com.starter.domain.entity.Subscription;
import com.starter.domain.entity.User;

import java.util.Optional;

/**
 * @author ilya
 * @date 08.11.2021
 */

public interface SubscriptionRepository extends Repository<Subscription> {

    Optional<Subscription> findOneByUser(User user);

}
