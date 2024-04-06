package com.starter.common.aspect.logging.extractor;

import com.starter.common.service.JwtProvider;
import com.starter.domain.entity.User;
import com.starter.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;


//@Component
//@RequiredArgsConstructor
//public class TokenUserExtractor implements UserExtractor {
//
//    private final JwtProvider jwtProvider;
//    private final UserRepository userRepository;
//
//    @Override
//    public UserQualifier extract(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
//        final var login = jwtProvider.getUserLoginFromRequest(request);
//        final var id = userRepository.findByLogin(login).map(User::getId).orElse(null);
//        return new UserQualifier(id, login);
//    }
//}
