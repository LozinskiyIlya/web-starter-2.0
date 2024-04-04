package com.starter.web.aspect.logging.extractor;

import com.starter.domain.entity.User;
import com.starter.domain.repository.UserRepository;
import com.starter.web.filter.JwtFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class TokenUserExtractor implements UserExtractor {

    private final JwtFilter jwtFilter;
    private final UserRepository userRepository;

    @Override
    public UserQualifier extract(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        final var login = jwtFilter.getUserLoginFromRequest(request);
        final var id = userRepository.findByLogin(login).map(User::getId).orElse(null);
        return new UserQualifier(id, login);
    }
}
