package com.starter.web.controller.auth;

import com.starter.common.aspect.logging.LogApiAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/session")
@Schema(title = "Session management")
@LogApiAction
public class SessionController {

    @GetMapping("/checkPin")
    public ResponseEntity<Boolean> checkPin(HttpSession session) {
        Boolean pinEntered = (Boolean) session.getAttribute("pinEntered");
        return ResponseEntity.ok(pinEntered != null && pinEntered);
    }

    @PostMapping("/setPin")
    public ResponseEntity<Void> setPin(HttpSession session) {
        session.setAttribute("pinEntered", true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resetPin")
    public ResponseEntity<Void> resetPin(HttpSession session) {
        session.setAttribute("pinEntered", false);
        return ResponseEntity.ok().build();
    }
}
