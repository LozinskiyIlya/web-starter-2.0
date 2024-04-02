package com.starter.web.controller.auth;

import com.starter.domain.entity.Role;
import com.starter.domain.entity.User;
import com.starter.domain.repository.Repository;
import com.starter.domain.repository.RoleRepository;
import com.starter.domain.repository.UserRepository;
import com.starter.domain.repository.testdata.UserTestData;
import com.starter.web.AbstractSpringIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIT extends AbstractSpringIntegrationTest implements UserTestData {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("Login")
    class Login {

        @Test
        @DisplayName("Invalid username")
        void invalidUserName() throws Exception {
            var authRequest = new AuthController.AuthRequest();
            authRequest.setLogin(randomEmail());
            authRequest.setPassword("password");
            mockMvc.perform(postRequest("/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(authRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().json("{\"error\":\"Invalid login or password\"}", true));
        }

        @Test
        @DisplayName("Invalid password")
        void invalidPassword() throws Exception {
            final var user = givenUserExists(u -> {
            });
            //when
            var authRequest = new AuthController.AuthRequest();
            authRequest.setLogin(user.getLogin());
            authRequest.setPassword("invalid password");
            mockMvc.perform(postRequest("/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(authRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().json("{\"error\":\"Invalid login or password\"}", true));
        }

        @Test
        @DisplayName("should let log in with uppercase and lower case login")
        void shouldLetLogInWithUpperAndLowerCaseEmail() throws Exception {
            //given
            final var email = randomEmail();
            givenUserExists(u -> {
                u.setLogin("ggg" + email);
                u.setPassword(passwordEncoder.encode("password"));
            });
            //try to log in with upper and lower case email
            var authRequest = new AuthController.AuthRequest();
            authRequest.setLogin("ggg" + email.toUpperCase());
            authRequest.setPassword("password");
            MvcResult result = mockMvc.perform(postRequest("/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(authRequest)))
                    .andExpect(status().isOk()).andReturn();
            assertTrue(result.getResponse().getContentAsString().contains("token"));

            //try to log in with lower case email
            authRequest = new AuthController.AuthRequest();
            authRequest.setLogin("GGG" + email.toLowerCase());
            authRequest.setPassword("password");
            result = mockMvc.perform(postRequest("/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(authRequest)))
                    .andExpect(status().isOk()).andReturn();
            assertTrue(result.getResponse().getContentAsString().contains("token"));
        }

        @Test
        @DisplayName("should return firstLogin = true")
        void shouldReturnFirstLoginTrue() throws Exception {
            final var user = givenUserExists(u -> {
                u.setLogin("ggg" + randomEmail());
                u.setPassword(passwordEncoder.encode("password"));
            });
            //when
            var authRequest = new AuthController.AuthRequest();
            authRequest.setLogin(user.getLogin());
            authRequest.setPassword("password");
            var content = mockMvc.perform(postRequest("/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(authRequest)))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            //then
            var authResponse = mapper.readValue(content, AuthController.AuthResponse.class);
            assertTrue(authResponse.getFirstLogin());
        }
    }

    @Nested
    @DisplayName("Register")
    class Register {

        @Test
        @DisplayName("Email already exists")
        void emailAlreadyExists() throws Exception {
            final var user = givenUserExists(u -> {
            });
            var registrationRequest = new AuthController.RegistrationRequest();
            registrationRequest.setEmail(user.getLogin());
            registrationRequest.setPassword(user.getPassword());
            mockMvc.perform(postRequest("/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(content().json("{\"error\":\"User with this email already exists\"}", true));
        }

        @Test
        @DisplayName("should let log in with differently cased email after registration")
        void shouldLetLogInWithDifferentCaseEmail() throws Exception {
            //given
            final var email = randomEmail();
            var registrationRequest = new AuthController.RegistrationRequest();
            registrationRequest.setEmail("ggg" + email);
            registrationRequest.setPassword("password");
            MvcResult result = mockMvc.perform(postRequest("/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isOk()).andReturn();
            assertTrue(result.getResponse().getContentAsString().contains("token"));

            //try to log in with differently cased email
            var authRequest = new AuthController.AuthRequest();
            authRequest.setLogin("GgG" + email.toUpperCase());
            authRequest.setPassword("password");
            result = mockMvc.perform(postRequest("/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(authRequest)))
                    .andExpect(status().isOk()).andReturn();
            assertTrue(result.getResponse().getContentAsString().contains("token"));
        }

        @Test
        @DisplayName("should return firstLogin = true")
        void shouldReturnFirstLoginTrue() throws Exception {
            var registrationRequest = new AuthController.RegistrationRequest();
            registrationRequest.setEmail(randomEmail());
            registrationRequest.setPassword("password");
            var content = mockMvc.perform(postRequest("/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(registrationRequest)))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            var authResponse = mapper.readValue(content, AuthController.AuthResponse.class);
            assertTrue(authResponse.getFirstLogin());
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
        return "/api/auth";
    }
}