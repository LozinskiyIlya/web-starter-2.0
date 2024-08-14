package com.starter.common;

import com.starter.common.job.TestJob;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;


@SpringBootTest
@Import(AbstractCommonSpringIntegrationTest.CommonSpringIntegrationTestConfig.class)
public class AbstractCommonSpringIntegrationTest extends AbstractLocalstackIntegrationTest {

    @TestConfiguration
    public static class CommonSpringIntegrationTestConfig {
        @Bean
        public TestJob testJob() {
            return new TestJob();
        }
    }
}
