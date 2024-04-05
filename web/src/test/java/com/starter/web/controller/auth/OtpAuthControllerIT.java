package com.starter.web.controller.auth;

import com.starter.domain.entity.Role;
import com.starter.domain.entity.User;
import com.starter.domain.repository.ApiActionRepository;
import com.starter.domain.repository.Repository;
import com.starter.domain.repository.RoleRepository;
import com.starter.domain.repository.UserRepository;
import com.starter.domain.repository.testdata.UserTestData;
import com.starter.web.AbstractSpringIntegrationTest;
import com.starter.web.controller.auth.AuthController.AuthRequest;
import com.starter.web.controller.auth.AuthController.AuthResponse;
import com.starter.web.controller.auth.OTPAuthController.OTPChallengeRequest;
import com.starter.web.service.auth.OTPAuthService;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OtpAuthControllerIT extends AbstractSpringIntegrationTest implements UserTestData {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ApiActionRepository apiActionRepository;

    @MockBean
    private OTPAuthService otpAuthService;

    @Nested
    @DisplayName("Challenge")
    class Challenge {

        @Test
        @DisplayName("should create user if does not exist")
        void shouldCreateUser() throws Exception {
            //given
            var challengeReq = new OTPChallengeRequest();
            challengeReq.setEmail(UUID.randomUUID() + "@gmail.com");
            //when
            mockMvc.perform(postRequest("/challenge")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(challengeReq)))
                    .andExpect(status().isOk());
            //then
            final var user = userRepository.findByLogin(challengeReq.getEmail());
            assertTrue(user.isPresent());
        }

        @Test
        @DisplayName("should work for existing user")
        void shouldWorkForExistingUser() throws Exception {
            var user = givenUserExists(u -> {
            });
            var challengeReq = new OTPChallengeRequest();
            challengeReq.setEmail(user.getLogin());
            mockMvc.perform(postRequest("/challenge")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(challengeReq)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Validate")
    class Validate {

        private static final String TOKEN = "token";
        private static final String CODE = "CODE12";

        @BeforeEach
        void setupMocks() {
            Mockito.when(otpAuthService.validate(Mockito.any(), Mockito.anyString())).thenReturn(TOKEN);
        }

        @Test
        @DisplayName("should not work if user is not found")
        void shouldNotWorkIfUserNotFound() throws Exception {
            var authReq = new AuthRequest();
            authReq.setEmail(UUID.randomUUID().toString());
            authReq.setPassword(CODE);
            mockMvc.perform(postRequest("/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(authReq)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should work for existing user")
        void shouldWorkForExistingUser() throws Exception {
            //given
            var user = givenUserExists(u -> {
            });
            var authReq = new AuthRequest();
            authReq.setEmail(user.getLogin());
            authReq.setPassword(CODE);
            //when
            final var response = mockMvc.perform(postRequest("/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(authReq)))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            //then
            final var authResp = mapper.readValue(response, AuthResponse.class);
            assertEquals(TOKEN, authResp.getToken());
        }
    }

    @Nested
    @DisplayName("ApiAction")
    class ApiAction {

        @Test
        @DisplayName("should save api action for challenge")
        void shouldSaveApiActionForChallenge() throws Exception {
            //given
            var user = givenUserExists(u -> {
            });
            var challengeReq = new OTPChallengeRequest();
            challengeReq.setEmail(user.getLogin());
            //when
            mockMvc.perform(postRequest("/challenge")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(challengeReq)))
                    .andExpect(status().isOk());
            //then
            await().atMost(2, SECONDS).until(() -> !apiActionRepository.findAllByUserId(user.getId()).isEmpty());
            final var actions = apiActionRepository.findAllByUserId(user.getId());
            assertEquals(1, actions.size());
            assertNull(actions.get(0).getError());
            assertEquals(user.getLogin(), actions.get(0).getUserQualifier());
            assertEquals("/api/auth/otp/challenge", actions.get(0).getPath());
            assertEquals("POST", actions.get(0).getMetadata().getHttpMethod());
        }

        @Test
        @DisplayName("should save api action for validate")
        void shouldSaveApiActionForValidate() throws Exception {
            //given
            var user = givenUserExists(u -> {
            });
            var authReq = new AuthRequest();
            authReq.setEmail(user.getLogin());
            authReq.setPassword("CODE12");
            //when
            mockMvc.perform(postRequest("/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(authReq)))
                    .andExpect(status().isOk());
            //then
            await().atMost(2, SECONDS).until(() -> !apiActionRepository.findAllByUserId(user.getId()).isEmpty());
            final var actions = apiActionRepository.findAllByUserId(user.getId());
            assertEquals(1, actions.size());
            assertNull(actions.get(0).getError());
            assertEquals(user.getLogin(), actions.get(0).getUserQualifier());
            assertEquals("/api/auth/otp/validate", actions.get(0).getPath());
            assertEquals("POST", actions.get(0).getMetadata().getHttpMethod());
        }
    }

    @Override
    public Repository<User> userRepository() {
        return userRepository;
    }

    @Override
    public Repository<Role> roleRepository() {
        return roleRepository;
    }

    @Override
    public String controllerPath() {
        return "/api/auth/otp";
    }
}