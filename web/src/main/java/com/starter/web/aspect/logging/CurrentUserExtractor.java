package com.starter.web.aspect.logging;

import com.starter.web.service.user.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class CurrentUserExtractor extends UserExtractor {

    private final CurrentUserService currentUserService;

    @Override
    UserQualifier extract(HttpServletRequest request, Object[] handlerArgs) {
        final var user = currentUserService.getUser().orElseThrow();
        return new UserQualifier(user.getId(), user.getLogin());
    }
}
