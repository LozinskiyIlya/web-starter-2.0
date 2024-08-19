package com.starter.web.controller.user;

import com.starter.common.aspect.logging.LogApiAction;
import com.starter.common.exception.Exceptions.*;
import com.starter.common.service.CurrentUserService;
import com.starter.web.service.user.PurgeUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.starter.domain.entity.Role.Roles.ADMIN;
import static com.starter.domain.entity.Role.Roles.INTERNAL_ADMIN;
import static com.starter.domain.entity.User.UserType.GOOGLE;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user/delete")
@Schema(title = "Удаление пользовательской информации")
@LogApiAction
public class DeleteUserController {

    private final PurgeUserService purgeUserService;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "Удалить данные пользователя")
    @DeleteMapping("/{userId}")
    void purgeUser(@PathVariable("userId") UUID userId,
                   @RequestBody PurgeUserDTO body) {
        var currentUser = currentUserService.getUser().orElseThrow(WrongUserException::new);
        if (ADMIN.getRoleName().equals(currentUser.getRole().getName()) ||
                INTERNAL_ADMIN.getRoleName().equals(currentUser.getRole().getName())) {
            //admin does what he wants
            purgeUserService.purgeUser(userId);
            return;
        }
        if (!userId.equals(currentUser.getId())) {
            //can delete only own account
            throw new WrongUserException();
        }
        if (currentUser.getUserType() == GOOGLE) {
            //google user has no password
            purgeUserService.purgeUser(userId);
            return;
        }
        if (!StringUtils.hasText(body.password)) {
            //user without password
            throw new MissingPasswordException();
        }
        if (!passwordEncoder.matches(body.password, currentUser.getPassword())) {
            // wrong password
            throw new WrongUserException();
        }
        purgeUserService.purgeUser(userId);
    }

    @Data
    static class PurgeUserDTO {

        private String password;
    }
}
