package com.starter.common.service;

import com.starter.domain.entity.Subscription;
import com.starter.domain.entity.User;
import com.starter.domain.repository.SubscriptionRepository;
import com.starter.domain.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserDetailsService loader;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    public static Optional<String> getUsername() {
        return Optional.of(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .map(Principal::getName);
    }

    public UserDetails getUserDetails() {
        return loader.loadUserByUsername(getUsername().orElseThrow());
    }

    public Optional<User> getUser() {
        return userRepository.findByLogin(getUsername().orElseThrow());
    }

    @Transactional
    public boolean isPremium() {
        final var current = getUser().orElseThrow();
        return subscriptionRepository.findOneByUser(current)
                .map(Subscription::isActive)
                .orElse(false);
    }
}
