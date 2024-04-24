package com.starter.web;

import com.starter.web.configuration.ServerProperties;
import com.starter.web.populator.Populator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;

import java.util.Collection;

@SpringBootApplication(scanBasePackages = "com.starter")
@EnableConfigurationProperties(ServerProperties.class)
public class WebStarterApplication {

    @Autowired
    private Collection<Populator> populators;

    @EventListener(ApplicationReadyEvent.class)
    public void setupApplication() {
        populators.forEach(Populator::populate);
    }

    public static void main(String[] args) {
        SpringApplication.run(WebStarterApplication.class, args);
    }

}
