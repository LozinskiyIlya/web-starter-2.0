package com.starter.web.service.auth;

import com.starter.common.service.JwtProvider;
import com.starter.domain.entity.User;
import com.starter.web.configuration.auth.OTPAuthConfig.OTPGenerator;
import com.starter.common.exception.Exceptions.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static java.time.Instant.now;


/**
 * @author ilya
 * @date 28.02.2023
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OTPAuthService {

    private final OTPGenerator generator;
    private final JwtProvider jwtProvider;


    @SneakyThrows
    // todo: use some entity with timestamp to make code valid for 5 full minutes, not 5 minutes window with fix start, like 00:05, 00:10, 00:15
    public String validate(User user, String code) {
        if (!generator.generate(user.getId(), now()).equals(code)) {
            throw new InvalidOtpException();
        }
        return jwtProvider.generateToken(user.getLogin());
    }

    @Transactional
    public void challenge(User user) {
        final var code = generator.generate(user.getId(), now());
        log.info("Generated code: {}", code);
        // todo send code to user
    }
}
