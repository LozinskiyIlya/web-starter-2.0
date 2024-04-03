package com.starter.web.filter.logging;


import com.starter.domain.entity.ApiAction;
import com.starter.domain.entity.ApiAction.Metadata;
import com.starter.domain.repository.ApiActionRepository;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionSaver {

    private final ApiActionRepository apiActionRepository;
    private final Map<Class<? extends UserExtractor>, UserExtractor> userExtractors = new HashMap<>();


    public void save(LogAction config, HttpServletRequest request, @Nullable Exception exception) {
        final var meta = extractMetadata(request, config.logParams());
        final var user = userExtractors.get(config.userExtractor()).extract(request);
        final var action = new ApiAction();
        action.setMetadata(meta);
        action.setUserId(user.id());
        action.setUserQualifier(user.qualifier());
        try {
            action.setPath(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString());
        } catch (NullPointerException e) {
            action.setPath(request.getServletPath());
        }
        if (exception != null) {
            action.setError(exception.toString());
        }
        apiActionRepository.saveAndFlush(action);
    }

    private Metadata extractMetadata(HttpServletRequest request, boolean logParams) {
        var metadata = new Metadata();
        metadata.setUserAgent(request.getHeader("User-Agent"));
        metadata.setHttpMethod(request.getMethod());
        var ipHeaderVal = request.getHeader("Public-Ip");
        metadata.setIp(StringUtils.hasText(ipHeaderVal) ? ipHeaderVal : request.getRemoteAddr());
        if (logParams) {
            metadata.setParams(request.getQueryString());
        }
        return metadata;
    }


    @Autowired
    private void setExtractorsMap(Collection<UserExtractor> userExtractors) {
        userExtractors.forEach(e -> this.userExtractors.put(e.getClass(), e));
    }
}
