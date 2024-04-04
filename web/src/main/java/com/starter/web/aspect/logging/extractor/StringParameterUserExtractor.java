package com.starter.web.aspect.logging.extractor;


import com.starter.domain.entity.User;
import com.starter.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
abstract class StringParameterUserExtractor extends ParameterUserExtractor<String> {
    private UserRepository userRepository;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    Class<String> getParameterType() {
        return String.class;
    }

    @Override
    UserQualifier getUserQualifier(String parameter) {
        return new UserQualifier(userRepository.findByLogin(parameter).map(User::getId).orElse(null), parameter);
    }

}
