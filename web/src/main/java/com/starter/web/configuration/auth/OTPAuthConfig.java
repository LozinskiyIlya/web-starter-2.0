package com.starter.web.configuration.auth;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.eatthepath.otp.UncheckedNoSuchAlgorithmException;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * @author ilya
 * @date 01.03.2023
 */

@Configuration
public class OTPAuthConfig {

    private static final int OTP_LENGTH = 6;
    private static final Duration OTP_DURATION = Duration.ofMinutes(5);

    @Bean
    public OTPGenerator timeBasedOneTimePasswordGenerator() {
        return new OTPGenerator();
    }

    public static final class OTPGenerator extends TimeBasedOneTimePasswordGenerator {
        public OTPGenerator() throws UncheckedNoSuchAlgorithmException {
            super(OTP_DURATION, OTP_LENGTH);
        }

        @SneakyThrows
        public String generateOtp(UUID seed, Instant timestamp) {
            final var key = generateOtpSpec(seed.toString());
            return super.generateOneTimePasswordString(key, timestamp);
        }

        @SneakyThrows
        private SecretKeySpec generateOtpSpec(String seed) {
            final var secret = seed.substring(seed.length() - 6);
            final var secretBytes = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            final var truncatedSecret = new byte[this.getPasswordLength()];
            System.arraycopy(secretBytes, 0, truncatedSecret, 0, this.getPasswordLength());
            return new SecretKeySpec(truncatedSecret, this.getAlgorithm());
        }
    }
}
