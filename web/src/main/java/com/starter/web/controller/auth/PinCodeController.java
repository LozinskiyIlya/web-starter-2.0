package com.starter.web.controller.auth;

import com.starter.common.aspect.logging.LogApiAction;
import com.starter.web.service.auth.PinCodeService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/pin")
@Schema(title = "Pin code management")
@LogApiAction
public class PinCodeController {

    private final PinCodeService pinCodeService;

    @PostMapping("")
    public Boolean verifyPin(@RequestBody @Valid PinAuthRequest authRequest, HttpSession session) {
        return pinCodeService.verifyPin(authRequest.getPin(), session);
    }

    @PostMapping("/reset")
    public void resetPin(HttpSession session) {
        pinCodeService.resetPin(session);
    }

    @Data
    @Schema(description = "DTO for verifying pin")
    public static class PinAuthRequest {
        @NotEmpty(message = "Pin is required")
        @Size(min = 6, max = 6, message = "Pin must be exactly 6 digits")
        @Pattern(regexp = "\\d{6}", message = "Pin can only contain digits")
        @Schema(description = "6-digits pin", example = "123456")
        private String pin;
    }
}
