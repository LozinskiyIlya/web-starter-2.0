package com.starter.web.filter;

import com.starter.web.populator.SwaggerUserPopulator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@RequiredArgsConstructor
public class SwaggerGuardingFilter extends OncePerRequestFilter {
    public static final String SWAGGER_UI_URL = "/swagger-ui/index.html";
    public static final String HEADER_VALUE = Base64.getEncoder().encodeToString(String.format("%s:%s", SwaggerUserPopulator.SWAGGER_USER, SwaggerUserPopulator.SWAGGER_PASSWORD).getBytes());
    public static final List<String> SWAGGER_URLS = List.of(SWAGGER_UI_URL, "/v2/api-docs");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        final String requestTokenHeader = request.getHeader("Authorization");
        if (SWAGGER_URLS.stream().anyMatch(suffix -> request.getRequestURI().endsWith(suffix))) {
            if (requestTokenHeader == null || !requestTokenHeader.startsWith("Basic ")) {
                // 401
                response.setHeader("WWW-Authenticate", "Basic");
                response.setStatus(401);
                return;
            } else if (!HEADER_VALUE.equals(requestTokenHeader.substring(6))) {
                // 401
                response.setHeader("WWW-Authenticate", "Basic");
                response.setStatus(401);
                return;
            }
        }
        //delegate - it's not the case
        chain.doFilter(request, response);
    }
}