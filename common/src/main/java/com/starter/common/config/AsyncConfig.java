package com.starter.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
@EnableTransactionManagement
public class AsyncConfig {

    @Bean
    public Executor taskExecutor() {
        return Executors.newCachedThreadPool();
    }
}
