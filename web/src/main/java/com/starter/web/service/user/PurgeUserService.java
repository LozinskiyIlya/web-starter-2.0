package com.starter.web.service.user;

import com.starter.domain.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Soft deletes all user-related data
 * Due to hibernate cascading restrictions, has to delete everything manually
 */
@Service
@RequiredArgsConstructor
public class PurgeUserService {

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final UserSettingsRepository userSettingsRepository;

    @Transactional
    public void purgeUser(UUID userId) {
        userRepository.findById(userId).ifPresent(u -> {
            var tombstone = Instant.now();
            userInfoRepository.deleteAll(userInfoRepository.findAllByUser(u));
            userSettingsRepository.deleteAll(userSettingsRepository.findAllByUser(u));
            u.setLogin(u.getLogin() + "[deleted:" + tombstone + "]");
            userRepository.saveAndFlush(u);
            userRepository.delete(u);
        });
    }
}
