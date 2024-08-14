package com.starter.common.config;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({S3Properties.class, CdnProperties.class})
public class AwsConfig {

    @Bean
    @Qualifier("starter")
    public AmazonS3 starterS3Client() {
        return AmazonS3ClientBuilder.standard()
                .withRegion("us-east-1")
                .build();
    }

}
