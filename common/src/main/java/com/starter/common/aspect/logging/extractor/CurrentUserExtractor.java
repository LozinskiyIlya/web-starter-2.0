package com.starter.common.aspect.logging.extractor;

import com.starter.common.exception.Exceptions;
import com.starter.common.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class CurrentUserExtractor implements UserExtractor {

    private final CurrentUserService currentUserService;

    @Override
    public UserQualifier extract(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        final var user = currentUserService.getUser().orElseThrow(Exceptions.WrongUserException::new);
        return new UserQualifier(user.getId(), user.getLogin());
    }
}
