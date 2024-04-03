package com.starter.web.aspect.logging;

import com.starter.web.aspect.logging.UserExtractor.UserQualifier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 1)
@Component
@RequiredArgsConstructor
public class ApiActionAspect {

    private final ApiActionSaver apiActionSaver;
    private final Map<Class<? extends UserExtractor>, UserExtractor> userExtractors = new HashMap<>();

    @Autowired
    private void setExtractorsMap(Collection<UserExtractor> userExtractors) {
        userExtractors.forEach(e -> this.userExtractors.put(e.getClass(), e));
    }

    @Around("@annotation(logApiAction)")
    public Object logRequestDetails(ProceedingJoinPoint joinPoint, LogApiAction logApiAction) throws Throwable {
        Object result;
        Exception exception = null;
        UserQualifier userQualifier = new UserQualifier(null, null);
        final var request = getRequest();
        try {
            userQualifier = userExtractors.get(logApiAction.userExtractor()).extract(request, joinPoint.getArgs());
            result = joinPoint.proceed();
        } catch (Exception e) {
            exception = e; // Capture exception if any
            throw e; // Re-throw the exception to keep the method's original behavior
        } finally {
            apiActionSaver.save(request, userQualifier, logApiAction.logParams(), exception);
        }

        return result;
    }

    private static HttpServletRequest getRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        } catch (NullPointerException e) {
            throw new IllegalStateException("@LogApiAction annotation is used outside of a web request context.");
        }
    }
}
