package com.starter.web.aspect.logging.extractor;


import com.starter.domain.entity.User;
import com.starter.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
abstract class UUIDParameterUserExtractor extends ParameterUserExtractor<UUID> {
    private UserRepository userRepository;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    Class<UUID> getParameterType() {
        return UUID.class;
    }

    @Override
    UserQualifier getUserQualifier(UUID parameter) {
        return new UserQualifier(userRepository.findById(parameter).map(User::getId).orElse(null), parameter.toString());
    }

}
