package com.starter.web.aspect.logging;

import com.starter.domain.entity.ApiAction;
import com.starter.domain.entity.ApiAction.Metadata;
import com.starter.domain.repository.ApiActionRepository;
import com.starter.web.aspect.logging.extractor.UserExtractor.UserQualifier;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiActionSaver {

    private final ApiActionRepository apiActionRepository;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    public void save(HttpServletRequest request, UserQualifier userQualifier, boolean saveParams, @Nullable Exception exception) {
        executor.submit(() -> save(request, userQualifier.id(), userQualifier.qualifier(), saveParams, exception));
    }

    private void save(HttpServletRequest request, UUID userId, String userQualifier, boolean saveParams, @Nullable Exception exception) {
        final var meta = extractMetadata(request, saveParams);
        final var action = new ApiAction();
        action.setMetadata(meta);
        action.setUserId(userId);
        action.setUserQualifier(userQualifier);
        try {
            action.setPath(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString());
        } catch (NullPointerException e) {
            action.setPath(request.getServletPath());
        }
        if (exception != null) {
            action.setError(exception.getMessage());
        }
        apiActionRepository.saveAndFlush(action);
    }

    private Metadata extractMetadata(HttpServletRequest request, boolean saveParams) {
        var metadata = new Metadata();
        metadata.setUserAgent(request.getHeader("User-Agent"));
        metadata.setHttpMethod(request.getMethod());
        var ipHeaderVal = request.getHeader("Public-Ip");
        metadata.setIp(StringUtils.hasText(ipHeaderVal) ? ipHeaderVal : request.getRemoteAddr());
        if (saveParams) {
            metadata.setParams(request.getQueryString());
        }
        return metadata;
    }
}
