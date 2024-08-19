package com.starter.web.controller;

import com.starter.common.aspect.logging.LogApiAction;
import com.starter.common.service.CurrentUserService;
import com.starter.domain.repository.SubscriptionRepository;
import com.starter.web.dto.SubscriptionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/subscription")
@Schema(title = "Premium subscription plan")
@LogApiAction
public class SubscriptionController {

    private final CurrentUserService currentUserService;
    private final SubscriptionRepository subscriptionRepository;

    @GetMapping("")
    @Operation(summary = "Returns current subscription")
    public SubscriptionDto getSubscription() {
        final var current = currentUserService.getUser().orElseThrow();
        return subscriptionRepository.findOneByUser(current)
                .map(SubscriptionDto::fromSubscription)
                .orElseGet(SubscriptionDto::EMPTY);
    }
}
