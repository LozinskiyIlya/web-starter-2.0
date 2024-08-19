package com.starter.web;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.starter.web.configuration.aws.CdnProperties;
import com.starter.web.configuration.aws.S3Properties;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

/**
 * Base class to initialize aws testcontainers
 */

@Import(AbstractLocalstackIntegrationTest.TestConfig.class)
abstract class AbstractLocalstackIntegrationTest {

    private static final DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:0.11.3");

    protected static final LocalStackContainer LOCALSTACK = new LocalStackContainer(localstackImage)
            .withServices(S3);
    protected static final AWSStaticCredentialsProvider CREDENTIALS_PROVIDER = new AWSStaticCredentialsProvider(new BasicAWSCredentials(
            LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey()
    ));

    private final Set<LocalStackContainer.Service> initializedAwsServices = new HashSet<>();

    @Autowired
    protected AmazonS3 s3;

    @Autowired
    protected S3Properties s3Properties;

    @Autowired
    protected CdnProperties cdnProperties;

    @BeforeAll
    static void startLocalstackContainer() {
        if (!LOCALSTACK.isRunning()) {
            LOCALSTACK.start();
            awaitTestCompletion().until(LOCALSTACK::isRunning);
        }
    }

    @DynamicPropertySource
    static void setCredentials(DynamicPropertyRegistry registry) {
        var credentials = CREDENTIALS_PROVIDER.getCredentials();
        registry.add("cloud.aws.region.auto", () -> false);
        registry.add("cloud.aws.region.static", LOCALSTACK::getRegion);
        registry.add("cloud.aws.s3.endpoint", () -> LOCALSTACK.getEndpointOverride(S3));
        registry.add("cloud.aws.credentials.access-key", credentials::getAWSAccessKeyId);
        registry.add("cloud.aws.credentials.secret-key", credentials::getAWSSecretKey);
    }


    @BeforeEach
    void createBucketIfNotExists() {
        if (!initializedAwsServices.contains(S3)) {
            s3.createBucket(s3Properties.getAttachmentBucketName());
            s3.createBucket(s3Properties.getAvatarBucketName());
            initializedAwsServices.add(S3);
        }
    }

    protected static ConditionFactory awaitTestCompletion() {
        return Awaitility.await().atMost(10, TimeUnit.SECONDS);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        @Qualifier("starter")
        public AmazonS3 s3() {
            return AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                            LOCALSTACK.getEndpointOverride(S3).toString(),
                            LOCALSTACK.getRegion()))
                    .withCredentials(CREDENTIALS_PROVIDER)
                    .build();
        }
    }
}
