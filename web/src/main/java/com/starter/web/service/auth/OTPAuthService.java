package com.starter.web.service.auth;

import com.starter.domain.entity.User;
import com.starter.web.configuration.auth.OTPAuthConfig.OTPGenerator;
import com.starter.web.controller.GlobalExceptionHandler.InvalidOtpException;
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
    public String validate(User user, String code) {
        if (!generator.generateOtp(user.getId(), now()).equals(code)) {
            throw new InvalidOtpException();
        }
        return jwtProvider.generateToken(user.getLogin());
    }

    @Transactional
    public void challenge(User user) {
        final var code = generator.generateOtp(user.getId(), now());
        log.info("Generated code: {}", code);
        // todo send code to user
    }
}
