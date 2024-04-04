package com.starter.web.aspect.logging.extractor;

import com.starter.web.controller.GlobalExceptionHandler;
import com.starter.web.service.user.CurrentUserService;
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
        final var user = currentUserService.getUser().orElseThrow(GlobalExceptionHandler.WrongUserException::new);
        return new UserQualifier(user.getId(), user.getLogin());
    }
}
