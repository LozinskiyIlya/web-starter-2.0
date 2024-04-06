package com.starter.common.aspect.logging.extractor;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.UUID;

/**
 * Extracts user id and/or other qualifier such as username from request
 * The id is optional because user may not be authenticated
 */
public interface UserExtractor {
    UserQualifier extract(HttpServletRequest request, ProceedingJoinPoint joinPoint);

    record UserQualifier(UUID id, String qualifier) {
        static UserQualifier empty() {
            return new UserQualifier(null, null);
        }
    }
}



