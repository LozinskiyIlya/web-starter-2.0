package com.starter.web.aspect.logging.extractor;

import com.starter.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginUserExtractor extends StringParameterUserExtractor {

    private final UserRepository userRepository;

    @Override
    String getParameterName() {
        return "login";
    }

    @Override
    UserQualifier getUserQualifier(String email) {
        return userRepository.findByLogin(email)
                .map(user -> new UserQualifier(user.getId(), user.getLogin()))
                .orElse(new UserQualifier(null, email));
    }
}
