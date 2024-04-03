package com.starter.web.filter.logging;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author ilya
 * @date 21.05.2022
 */
@Slf4j
@RequiredArgsConstructor
public class LogActionInterceptor implements HandlerInterceptor {

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final ActionSaver actionSaver;

    @PreDestroy
    void stopPools() {
        try {
            executor.shutdown();
            executor.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            log.error("Failed to stop logging api action executor", ex);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Mark the request if the handler method has {@link LogAction} annotation
     *
     * @see CachingRequestBodyFilter it will cache request body if the request was marked
     * we will use this cache to access request body in {@link #afterCompletion}
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            boolean annotated = handlerMethod.getMethod().isAnnotationPresent(LogAction.class);
            if (annotated) {
                request.setAttribute(LogAction.class.getSimpleName(), Boolean.TRUE);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            if (handlerMethod.hasMethodAnnotation(LogAction.class)) {
                executor.submit(() -> {
                    LogAction actionAnnotation = handlerMethod.getMethodAnnotation(LogAction.class);
                    if (actionAnnotation != null) {
                        actionSaver.save(
                                actionAnnotation,
                                request,
                                ex
                        );
                    }
                });
            }
        }
    }
}


