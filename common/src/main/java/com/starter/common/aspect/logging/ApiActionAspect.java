package com.starter.common.aspect.logging;

import com.starter.common.aspect.logging.extractor.UserExtractor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Aspect
@Order(HIGHEST_PRECEDENCE + 1)
@Component
@RequiredArgsConstructor
public class ApiActionAspect {

    private final ApiActionSaver apiActionSaver;
    private final Map<Class<? extends UserExtractor>, UserExtractor> userExtractors = new HashMap<>();

    @Autowired
    private void setExtractorsMap(Collection<UserExtractor> userExtractors) {
        userExtractors.forEach(e -> this.userExtractors.put(e.getClass(), e));
    }

    @Around("@within(LogApiAction) || @annotation(LogApiAction)")
    public Object logRequestDetails(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result;
        Exception exception = null;
        UserExtractor.UserQualifier userQualifier = new UserExtractor.UserQualifier(null, null);
        LogApiAction config = getAnnotationConfig(joinPoint);
        final var request = getRequest();
        try {
            userQualifier = userExtractors.get(config.userExtractor()).extract(request, joinPoint);
            result = joinPoint.proceed();
        } catch (Exception e) {
            exception = e; // Capture exception if any
            throw e; // Re-throw the exception to keep the method's original behavior
        } finally {
            apiActionSaver.save(request, userQualifier, config.logParams(), exception);
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

    private static LogApiAction getAnnotationConfig(ProceedingJoinPoint joinPoint) {
        LogApiAction methodAnnotation = ((MethodSignature) joinPoint.getSignature()).getMethod().getAnnotation(LogApiAction.class);
        LogApiAction classAnnotation = joinPoint.getTarget().getClass().getAnnotation(LogApiAction.class);
        return methodAnnotation != null ? methodAnnotation : classAnnotation;
    }
}
