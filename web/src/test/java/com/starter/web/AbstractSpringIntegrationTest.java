package com.starter.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import com.starter.common.service.JwtProvider;
import com.starter.domain.entity.Role;
import com.starter.domain.entity.User;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.RoleRepository;
import com.starter.domain.repository.UserRepository;
import com.starter.web.fragments.BillAssistantResponse;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.util.ArrayList;
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
    protected final EasyRandom random = new EasyRandom(new EasyRandomParameters().seed(System.nanoTime()));

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

    @Autowired
    protected ObjectMapper mapper;

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

    protected final Pair<String, String> userAuthHeaderUnchecked() {
        return Pair.of("Authorization", "Bearer " + jwtProvider.generateToken(UUID.randomUUID().toString()));
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

    protected BillAssistantResponse assistantResponse(String currency, Double amount) {
        final var response = BillAssistantResponse.EMPTY();
        response.setCurrency(currency);
        response.setAmount(amount);
        response.setTags(new String[]{"Work"});
        return response;
    }


    @TestConfiguration
    static class AbstractSpringIntegrationTestConfig {

        @Autowired
        private ObjectMapper mapper;

        @PostConstruct
        void addModules() {
            SimpleModule module = new SimpleModule();
            module.addDeserializer(BillRepository.TagAmount.class, new TagAmountDeserializer());
            mapper.registerModule(module);
        }
    }

    protected static class TagAmountDeserializer extends JsonDeserializer<BillRepository.TagAmount> {

        @Override
        public BillRepository.TagAmount deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            JsonNode node = jp.getCodec().readTree(jp);
            String name = node.get("name").asText();
            String hexColor = node.get("hexColor").asText();
            Double amount = node.get("amount").asDouble();

            return new BillRepository.TagAmount() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public String getHexColor() {
                    return hexColor;
                }

                @Override
                public Double getAmount() {
                    return amount;
                }
            };
        }
    }

    protected static class RestResponsePage<T> extends PageImpl<T> {

        @Serial
        private static final long serialVersionUID = 3248189030448292002L;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public RestResponsePage(@JsonProperty("content") List<T> content, @JsonProperty("number") int number, @JsonProperty("size") int size,
                                @JsonProperty("totalElements") Long totalElements, @JsonProperty("pageable") JsonNode pageable, @JsonProperty("last") boolean last,
                                @JsonProperty("totalPages") int totalPages, @JsonProperty("sort") JsonNode sort, @JsonProperty("first") boolean first,
                                @JsonProperty("numberOfElements") int numberOfElements) {
            super(content, PageRequest.of(number, size), totalElements);
        }

        public RestResponsePage(List<T> content, Pageable pageable, long total) {
            super(content, pageable, total);
        }

        public RestResponsePage(List<T> content) {
            super(content);
        }

        public RestResponsePage() {
            super(new ArrayList<T>());
        }
    }
}
