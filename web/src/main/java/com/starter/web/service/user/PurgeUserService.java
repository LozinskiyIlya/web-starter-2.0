package com.starter.web.service.user;

import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.GroupRepository;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.domain.repository.UserRepository;
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
    private final GroupRepository groupRepository;
    private final BillRepository billRepository;

    @Transactional
    public void purgeUser(UUID userId) {
        userRepository.findById(userId).ifPresent(u -> {
            var tombstone = Instant.now();
            userInfoRepository.deleteAll(userInfoRepository.findAllByUser(u));
            groupRepository.findAllByUser(u).forEach(g -> {
                billRepository.deleteAll(billRepository.findAllByGroup(g));
                groupRepository.delete(g);
            });
            u.setLogin(u.getLogin() + "[deleted:" + tombstone + "]");
            userRepository.saveAndFlush(u);
            userRepository.delete(u);
        });
    }
}
