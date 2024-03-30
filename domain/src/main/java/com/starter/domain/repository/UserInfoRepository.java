package com.starter.domain.repository;


import com.starter.domain.entity.User;
import com.starter.domain.entity.UserInfo;
import jakarta.transaction.Transactional;

import java.util.Optional;

/**
 * @author ilya
 * @date 27.08.2021
 */

@org.springframework.stereotype.Repository
@Transactional
public interface UserInfoRepository extends Repository<UserInfo> {

    Optional<UserInfo> findOneByUser(User user);
}
