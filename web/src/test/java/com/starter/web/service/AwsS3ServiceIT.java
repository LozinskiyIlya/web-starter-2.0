package com.starter.web.service;

import com.starter.domain.repository.testdata.BillTestDataCreator;
import com.starter.domain.repository.testdata.UserTestDataCreator;
import com.starter.web.AbstractSpringIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AwsS3ServiceIT extends AbstractSpringIntegrationTest {

    @Autowired
    private UserTestDataCreator userCreator;

    @Autowired
    private BillTestDataCreator billCreator;

    @Autowired
    private AwsS3Service awsS3Service;

    @Test
    @DisplayName("uploads avatar")
    void uploadsAvatar() throws IOException {
        //given
        final var user = userCreator.givenUserExists();
        final var image = new ClassPathResource("files/jpg/jerry.jpg");
        final var mockMultipart = new MockMultipartFile("file", "qwe/rty/cool_image.jpeg", "image/jpeg", image.getInputStream());
        //when
        final var uri = awsS3Service.uploadAvatar(user, mockMultipart);
        //then
        final var key = uri.toString().replace(cdnProperties.getHost().toString() + "/", "");
        final var metadata = s3.getObjectMetadata(s3Properties.getAvatarBucketName(), key);
        //and s3 metadata has proper content type
        assertEquals("image/jpeg", metadata.getContentType());
    }

    @Test
    @DisplayName("uploads attachment")
    void uploadsAttachment() throws IOException {
        //given
        final var bill = billCreator.givenBillExists();
        final var image = new ClassPathResource("files/pdf/Invoice3.pdf");
        final var mockMultipart = new MockMultipartFile("file", "qwe/rty/cool_invoice.pdf", "application/pdf", image.getInputStream());
        //when
        final var uri = awsS3Service.uploadAttachment(bill, mockMultipart);
        //then
        final var key = uri.toString().replace(cdnProperties.getHost().toString() + "/", "");
        final var metadata = s3.getObjectMetadata(s3Properties.getAttachmentBucketName(), key);
        //and s3 metadata has proper content type
        assertEquals("application/pdf", metadata.getContentType());
    }
}
