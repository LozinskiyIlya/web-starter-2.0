package com.starter.web.aspect.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * Extracts user id and/or other qualifier such as username from request
 * The id is optional because user may not be authenticated
 */
public abstract class UserExtractor {
    abstract UserQualifier extract(HttpServletRequest request, Object[] handlerArgs);

    public record UserQualifier(UUID id, String qualifier) {
    }

    protected String readBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper requestWrapper) {
            byte[] buf = requestWrapper.getContentAsByteArray();
            if (buf.length > 0) {
                try {
                    return new String(buf, request.getCharacterEncoding());
                } catch (UnsupportedEncodingException e) {
                    return "";
                }
            }
        }
        return "";
    }
}



