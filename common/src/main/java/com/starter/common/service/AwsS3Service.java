package com.starter.common.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.starter.common.config.CdnProperties;
import com.starter.common.config.S3Properties;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.User;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

import static java.util.UUID.randomUUID;

/**
 * Persist files to file storage
 */
@Service
public class AwsS3Service {
    private static final String APP_FOLDER = "ai-counting";
    private static final String AVATAR_PREFIX = "avatar-";
    private static final String ATTACHMENT_PREFIX = "attach-";
    private final AmazonS3 client;
    private final S3Properties properties;
    private final CdnProperties cdnProperties;

    public AwsS3Service(@Qualifier("starter") AmazonS3 client,
                        S3Properties properties,
                        CdnProperties cdnProperties) {
        this.client = client;
        this.properties = properties;
        this.cdnProperties = cdnProperties;
    }

    @SneakyThrows
    public URI uploadAvatar(User user, MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new IllegalArgumentException();
        }
        String extension = originalName.substring(originalName.lastIndexOf('.'));
        var key = s3AvatarFileKey(user, AVATAR_PREFIX + randomUUID() + extension);
        var metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());
        try (var stream = file.getInputStream()) {
            client.putObject(new PutObjectRequest(properties.getAvatarBucketName(), key, stream, metadata));
        }
        return cdnProperties.getHost().resolve("/").resolve(key);
    }

    @SneakyThrows
    public URI uploadAttachment(Bill bill, MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new IllegalArgumentException();
        }
        String extension = originalName.substring(originalName.lastIndexOf('.'));
        var key = s3AttachmentFileKey(bill, ATTACHMENT_PREFIX + randomUUID() + extension);
        var metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());
        try (var stream = file.getInputStream()) {
            client.putObject(new PutObjectRequest(properties.getAttachmentBucketName(), key, stream, metadata));
        }
        return cdnProperties.getHost().resolve("/").resolve(key);
    }

    private String s3AvatarFolderKey(User user) {
        return String.format("%s/users/%s/avatar/", APP_FOLDER, user.getId());
    }

    private String s3AttachmentFolderKey(Bill bill) {
        return String.format("%s/bills/%s/attach/", APP_FOLDER, bill.getId());
    }

    private String s3AttachmentFileKey(Bill bill, String filename) {
        var split = filename.split("/");
        var appendix = split[split.length - 1];
        return s3AttachmentFolderKey(bill) + appendix;
    }

    private String s3AvatarFileKey(User user, String filename) {
        var split = filename.split("/");
        var appendix = split[split.length - 1];
        return s3AvatarFolderKey(user) + appendix;
    }
}
