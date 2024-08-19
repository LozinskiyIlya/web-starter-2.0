package com.starter.web.dto;


import com.starter.domain.entity.Subscription;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

import static java.time.Instant.now;

@Data
public class SubscriptionDto {

    @NotNull
    private Double price;

    @NotNull
    private String currency;

    @NotNull
    private Instant createdAt;

    @NotNull
    private Instant endsAt;

    @NotNull
    private boolean active;

    public static SubscriptionDto EMPTY() {
        SubscriptionDto dto = new SubscriptionDto();
        dto.setActive(false);
        return dto;
    }

    public static SubscriptionDto fromSubscription(Subscription subscription) {
        SubscriptionDto dto = new SubscriptionDto();
        dto.setPrice(subscription.getPrice());
        dto.setCurrency(subscription.getCurrency());
        dto.setCreatedAt(subscription.getCreatedAt());
        dto.setEndsAt(subscription.getEndsAt());
        dto.setActive(subscription.isActive());
        return dto;
    }
}
