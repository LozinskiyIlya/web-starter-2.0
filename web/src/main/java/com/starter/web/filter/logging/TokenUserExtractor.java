package com.starter.web.filter.logging;

import com.starter.domain.entity.User;
import com.starter.domain.repository.UserRepository;
import com.starter.web.filter.JwtFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;


@Component
@RequiredArgsConstructor
public class TokenUserExtractor implements UserExtractor {

    private final JwtFilter jwtFilter;
    private final UserRepository userRepository;

    @Override
    public Pair<Optional<UUID>, String> extract(HttpServletRequest request, Object[] handlerArgs) {
        final var login = jwtFilter.getUserLoginFromRequest(request);
        final var id = userRepository.findByLogin(login).map(User::getId);
        return Pair.of(id, login);
    }
}
