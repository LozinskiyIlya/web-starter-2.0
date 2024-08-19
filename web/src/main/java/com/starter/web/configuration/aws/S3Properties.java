package com.starter.web.configuration.aws;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@Data
@Validated
@ConfigurationProperties("starter.aws.s3")
public class S3Properties {

    @NotBlank
    private String avatarBucketName;

    @NotBlank
    private String attachmentBucketName;

    private String accessKey;
    private String secretKey;
}
