package com.starter.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DirectoryConfig {

    @Bean(name = "downloadDirectory")
    @Profile({"local", "unknown", "test", "default"})
    public String downloadDirectoryLocal() {
        return System.getProperty("java.io.tmpdir");
    }

    @Bean(name = "downloadDirectory")
    @Profile({"dev", "prod"})
    public String downloadDirectoryRemote() {
        return "/downloads";
    }
}
