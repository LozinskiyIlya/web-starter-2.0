package com.starter.web.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
public class JacksonConfig {

    private final ObjectMapper mapper;

    @PostConstruct
    void addModules() {
        mapper.findAndRegisterModules();
    }

}
