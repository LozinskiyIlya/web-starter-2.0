package com.starter.domain.repository.testdata;


import com.starter.domain.entity.Subscription;
import com.starter.domain.repository.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Consumer;

public interface SubscriptionTestData {

    Repository<Subscription> subscriptionRepository();

    default Subscription givenSubscriptionExists(Consumer<Subscription> configure) {
        var subscription = new Subscription();
        subscription.setCurrency("USD");
        subscription.setPrice(10.0);
        subscription.setEndsAt(LocalDateTime.now().plusMonths(1).toInstant(ZoneOffset.UTC));
        configure.accept(subscription);
        return subscriptionRepository().saveAndFlush(subscription);
    }
}
