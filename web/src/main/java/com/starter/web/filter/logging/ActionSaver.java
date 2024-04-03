package com.starter.web.filter.logging;


import com.starter.domain.repository.ApiActionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionSaver {

    private final ApiActionRepository logActionRepository;
    private final Collection<UserExtractor> userExtractors;


    public void save(HttpServletRequest request, boolean b, Exception ex) {
    }
}
