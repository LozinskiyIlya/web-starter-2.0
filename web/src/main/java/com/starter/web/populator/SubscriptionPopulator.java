package com.starter.web.populator;


import com.starter.domain.entity.Subscription;
import com.starter.domain.entity.UserInfo;
import com.starter.domain.repository.SubscriptionRepository;
import com.starter.domain.repository.UserInfoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionPopulator implements Populator {

    private final UserInfoRepository userInfoRepository;
    private final SubscriptionRepository subscriptionRepository;


    @Override
    @Transactional
    public void populate() {
        userInfoRepository.findByTelegramChatId(424808989L)
                .map(UserInfo::getUser)
                .ifPresent(u -> {
                    final var myPremium = subscriptionRepository.findOneByUser(u)
                            .orElseGet(() -> {
                                        final var subscription = new Subscription();
                                        subscription.setUser(u);
                                        subscription.setPrice(10.0);
                                        subscription.setCurrency("USD");
                                        return subscription;
                                    }
                            );
                    myPremium.setEndsAt(LocalDateTime.now().plusYears(1).toInstant(ZoneOffset.UTC));
                    subscriptionRepository.save(myPremium);
                });
    }
}
