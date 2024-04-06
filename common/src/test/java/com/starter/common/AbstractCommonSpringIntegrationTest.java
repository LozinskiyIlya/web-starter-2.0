package com.starter.common;

import com.starter.common.job.TestJob;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;


@SpringBootTest
public class AbstractCommonSpringIntegrationTest {

    @TestConfiguration
    public static class CommonSpringIntegrationTestConfig {
        @Bean
        public TestJob testJob() {
            return new TestJob();
        }
    }
}
