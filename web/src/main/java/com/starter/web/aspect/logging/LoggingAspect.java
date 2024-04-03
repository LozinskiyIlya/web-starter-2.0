package com.starter.web.aspect.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class LoggingAspect {

    @Around("@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object logRequestDetails(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();

        // Extracting request details
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String httpMethod = request.getMethod();
        String params = request.getQueryString(); // For GET requests; for POST, you might need to read the body differently

        try {
            Object result = joinPoint.proceed(); // Proceed with the actual controller method
            return result;
        } finally {
            // You can log the extracted details here or perform further actions with them
            // Note: At this point, directly getting the response status code is not feasible as explained
        }
    }
}
