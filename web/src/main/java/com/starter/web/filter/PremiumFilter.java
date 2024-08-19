package com.starter.web.filter;

import com.starter.common.service.CurrentUserService;
import com.starter.domain.entity.Subscription;
import com.starter.domain.repository.SubscriptionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author ilya
 * @date 08.11.2021
 */
@Component
@Log
@RequiredArgsConstructor
public class PremiumFilter extends OncePerRequestFilter {

    private RequestMappingHandlerMapping handlerMapping;
    private final CurrentUserService currentUserService;
    private final SubscriptionRepository subscriptionRepository;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    public void setHandlerMapping(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @SneakyThrows
    @Override
    public void doFilterInternal(HttpServletRequest servletRequest,
                                 HttpServletResponse servletResponse,
                                 FilterChain filterChain) {

        // Check if handler is annotated with premium
        // It can be on controller level or a method
        // If it is, check if user has a subscription
        // If not, throw an 403 error
        HandlerExecutionChain handlerExecutionChain = handlerMapping.getHandler(servletRequest);
        Object handler = handlerExecutionChain != null ? handlerExecutionChain.getHandler() : null;

        if (handler instanceof HandlerMethod handlerMethod) {
            boolean isAnnotated = handlerMethod.getMethod().isAnnotationPresent(Premium.class) ||
                    handlerMethod.getBeanType().isAnnotationPresent(Premium.class);
            if (isAnnotated) {
                final var current = currentUserService.getUser().orElseThrow();
                final var isPremium = subscriptionRepository.findOneByUser(current)
                        .map(Subscription::isActive)
                        .orElse(false);
                if (!isPremium) {
                    final var annotation = getAnnotationInstance(handlerMethod);
                    servletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, annotation.value());
                    return;
                }
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private Premium getAnnotationInstance(HandlerMethod handlerMethod) {
        final var methodAnnotation = handlerMethod.getMethodAnnotation(Premium.class);
        final var classAnnotation = handlerMethod.getBeanType().getAnnotation(Premium.class);
        return methodAnnotation != null ? methodAnnotation : classAnnotation;
    }


    @Target({METHOD, TYPE})
    @Retention(RUNTIME)
    public @interface Premium {
        String value() default "This action is only available for premium users.";
    }
}
