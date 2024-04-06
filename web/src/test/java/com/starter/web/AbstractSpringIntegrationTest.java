package com.starter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import com.starter.domain.entity.Role;
import com.starter.domain.entity.User;
import com.starter.domain.repository.RoleRepository;
import com.starter.domain.repository.UserRepository;
import com.starter.common.service.JwtProvider;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.util.Pair;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static com.starter.domain.entity.Role.Roles.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractSpringIntegrationTest.AbstractSpringIntegrationTestConfig.class)
public abstract class AbstractSpringIntegrationTest {

    protected final String TEST_USER = "test_user";
    protected final String TEST_ADMIN = "test_admin";
    protected final String TEST_INTERNAL_ADMIN = "test_internal_admin";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    protected static final ObjectMapper mapper = new ObjectMapper();

    protected String controllerPath() {
        throw new UnsupportedOperationException("Define controller path for this test");
    }

    protected MockHttpServletRequestBuilder getRequest(String path) {
        return get(controllerPath() + path);
    }

    protected MockHttpServletRequestBuilder postRequest(String path) {
        return post(controllerPath() + path);
    }

    protected MockHttpServletRequestBuilder putRequest(String path) {
        return put(controllerPath() + path);
    }

    protected MockHttpServletRequestBuilder deleteRequest(String path) {
        return delete(controllerPath() + path);
    }

    protected MockMultipartHttpServletRequestBuilder multipartPostRequest(String path) {
        return multipart(controllerPath() + path);
    }

    @SneakyThrows
    public static String readResource(String filename) {
        var template = new ClassPathResource(filename);
        var text = "";
        try (InputStream html = template.getInputStream()) {
            ByteSource byteSource = new ByteSource() {
                @Override
                public InputStream openStream() {
                    return html;
                }
            };
            text = byteSource.asCharSource(Charsets.UTF_8).read();
        }
        return text;
    }

    @BeforeEach
    void setupTestUserIfMissing() {
        if (userRepository.findByLogin(TEST_USER).isEmpty()) {
            var user = new User();
            user.setLogin(TEST_USER);
            user.setPassword("password");
            user.setFirstLogin(false);
            user.setRole(roleRepository.findByName(USER.getRoleName()).orElseGet(() -> {
                var roleUser = new Role();
                roleUser.setName(USER.getRoleName());
                return roleRepository.save(roleUser);
            }));
            userRepository.save(user);
        }
    }

    @BeforeEach
    void setupTestAdminIfMissing() {
        if (userRepository.findByLogin(TEST_ADMIN).isEmpty()) {
            var admin = new User();
            admin.setLogin(TEST_ADMIN);
            admin.setPassword("password");
            admin.setFirstLogin(false);
            admin.setRole(roleRepository.findByName(ADMIN.getRoleName()).orElseGet(() -> {
                var roleAdmin = new Role();
                roleAdmin.setName(ADMIN.getRoleName());
                return roleRepository.save(roleAdmin);
            }));
            userRepository.save(admin);
        }
    }

    @BeforeEach
    void setupTestInternalAdminIfMissing() {
        if (userRepository.findByLogin(TEST_INTERNAL_ADMIN).isEmpty()) {
            var internalAdmin = new User();
            internalAdmin.setLogin(TEST_INTERNAL_ADMIN);
            internalAdmin.setPassword("password");
            internalAdmin.setFirstLogin(false);
            internalAdmin.setRole(roleRepository.findByName(INTERNAL_ADMIN.getRoleName()).orElseGet(() -> {
                var roleInternalAdmin = new Role();
                roleInternalAdmin.setName(INTERNAL_ADMIN.getRoleName());
                return roleRepository.save(roleInternalAdmin);
            }));
            userRepository.save(internalAdmin);
        }
    }

    protected String randomEmail() {
        return UUID.randomUUID() + "@gmail.com";
    }

    protected final String userToken(User user) {
        return jwtProvider.generateToken(user.getLogin());
    }

    protected final Pair<String, String> userAuthHeader(User user) {
        return userAuthHeader(user.getLogin());
    }

    protected final Pair<String, String> userAuthHeaderUnchecked(String login) {
        return Pair.of("Authorization", "Bearer " + jwtProvider.generateToken(login));
    }

    protected Pair<String, String> userAuthHeader(String login) {
        userRepository.findByLogin(login).orElseThrow();
        return Pair.of("Authorization", "Bearer " + jwtProvider.generateToken(login));
    }

    protected Pair<String, String> testUserAuthHeader() {
        return Pair.of("Authorization", "Bearer " + jwtProvider.generateToken(TEST_USER));
    }

    protected Pair<String, String> testAdminAuthHeader() {
        return Pair.of("Authorization", "Bearer " + jwtProvider.generateToken(TEST_ADMIN));
    }

    protected Pair<String, String> testInternalAdminAuthHeader() {
        return Pair.of("Authorization", "Bearer " + jwtProvider.generateToken(TEST_INTERNAL_ADMIN));
    }

    protected <T extends Comparable<T>> boolean isSortedDescending(List<T> list) {
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i - 1).compareTo(list.get(i)) < 0) {
                return false;
            }
        }
        return true;
    }


    @TestConfiguration
    static class AbstractSpringIntegrationTestConfig {
    }
}
