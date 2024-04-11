package com.starter.web;

import com.starter.domain.repository.BillTagRepository;
import com.starter.domain.repository.UserRepository;
import com.starter.openai.config.AssistantProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static com.starter.domain.entity.BillTag.TagType;
import static com.starter.web.populator.SwaggerUserPopulator.SWAGGER_USER;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WebStarterApplicationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BillTagRepository billTagRepository;

    @Autowired
    private AssistantProperties assistantProperties;

    @Test
    void swaggerUserPresent() {
        assertTrue(userRepository.findByLogin(SWAGGER_USER).isPresent());
    }

    @Test
    void defaultTagsPresent() {
        Arrays.stream(assistantProperties.getBillTags())
                .forEach(tag -> assertTrue(billTagRepository.findByNameAndTagType(tag, TagType.DEFAULT).isPresent()));
    }
}