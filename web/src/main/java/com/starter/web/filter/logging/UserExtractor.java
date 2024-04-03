package com.starter.web.filter.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.util.Pair;

import java.util.Optional;
import java.util.UUID;

/**
 * Extracts user id and/or other qualifier such as username from request
 * The id is optional because user may not be authenticated
 */
public interface UserExtractor {
    Pair<Optional<UUID>, String> extract(HttpServletRequest request, Object[] handlerArgs);
}
