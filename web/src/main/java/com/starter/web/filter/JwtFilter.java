package com.starter.web.filter;

import com.starter.web.service.auth.CustomUserDetails;
import com.starter.web.service.auth.CustomUserDetailsService;
import com.starter.web.service.auth.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.springframework.util.StringUtils.hasText;

/**
 * @author ilya
 * @date 08.11.2021
 */
@Component
@Log
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";

    private final JwtProvider jwtProvider;

    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public void doFilterInternal(HttpServletRequest servletRequest,
                                 HttpServletResponse servletResponse,
                                 FilterChain filterChain) throws ServletException, IOException {
        String token = getTokenFromRequest(servletRequest);
        if (token != null && jwtProvider.validateToken(token)) {
            String userLogin = jwtProvider.getLoginFromToken(token);
            try {
                CustomUserDetails customUserDetails = customUserDetailsService.loadUserByUsername(userLogin);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (UsernameNotFoundException ex) {
                servletResponse.setStatus(403);
                return;
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    public String getUserLoginFromRequest(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token != null && jwtProvider.validateToken(token)) {
            return jwtProvider.getLoginFromToken(token);
        }
        return null;
    }

    private static String getTokenFromRequest(HttpServletRequest request) {
        String bearer = request.getHeader(AUTHORIZATION);
        if (hasText(bearer) && bearer.startsWith(BEARER)) {
            return bearer.substring(7);
        }
        return null;
    }
}
