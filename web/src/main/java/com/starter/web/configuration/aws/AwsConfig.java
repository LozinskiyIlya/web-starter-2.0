package com.starter.web.configuration.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({S3Properties.class, CdnProperties.class})
public class AwsConfig {

    @Autowired
    private S3Properties s3Properties;

    @Bean
    @Qualifier("starter")
    public AmazonS3 starterS3Client() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(
                s3Properties.getAccessKey(),
                s3Properties.getSecretKey()
        );
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(
                        awsCredentials
                ))
                .withRegion("us-east-1")
                .build();
    }

}
