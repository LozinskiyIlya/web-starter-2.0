package com.starter.web.populator;

import com.starter.domain.entity.User;
import com.starter.domain.repository.RoleRepository;
import com.starter.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.starter.domain.entity.Role.Roles.INTERNAL_ADMIN;


@Component
@RequiredArgsConstructor
public class SwaggerUserPopulator implements Populator {

    public static final String SWAGGER_USER = "swagger_pierate";
    public static final String SWAGGER_PASSWORD = "securityisourpriority";

    private final PasswordEncoder encoder;
    private final UserRepository repo;
    private final RoleRepository roleRepository;

    @Override
    public void populate() {
        final Optional<User> swaggerUser = repo.findByLogin(SWAGGER_USER);
        if (swaggerUser.isPresent()) {
            return;
        }
        User user = new User();
        user.setLogin(SWAGGER_USER);
        user.setRole(roleRepository.findByName(INTERNAL_ADMIN.getRoleName()).orElseThrow());
        user.setPassword(encoder.encode(SWAGGER_PASSWORD));
        repo.save(user);
    }
}