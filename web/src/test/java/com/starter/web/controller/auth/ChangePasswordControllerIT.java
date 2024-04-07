package com.starter.web.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starter.domain.entity.Role;
import com.starter.domain.entity.User;
import com.starter.domain.repository.ApiActionRepository;
import com.starter.domain.repository.Repository;
import com.starter.domain.repository.RoleRepository;
import com.starter.domain.repository.UserRepository;
import com.starter.domain.repository.testdata.UserTestData;
import com.starter.web.AbstractSpringIntegrationTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class ChangePasswordControllerIT extends AbstractSpringIntegrationTest implements UserTestData {


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ApiActionRepository apiActionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("On requesting passwordChange")
    class OnPasswordChangeRequest {

        @Test
        @SneakyThrows
        @DisplayName("Save api action")
        void saveApiAction() {
            var user = givenUserExists(u -> {
            });
            mockMvc.perform(postRequest("/recovery?login=" + user.getLogin()))
                    .andExpect(status().isOk());
            // saving api action entity is async
            await().atMost(2, SECONDS).until(() -> !apiActionRepository.findAllByUserId(user.getId()).isEmpty());
            final var apiActions = apiActionRepository.findAllByUserId(user.getId());
            assertEquals(1, apiActions.size());
            final var changeAction = apiActions.get(0);
            assertEquals(user.getLogin(), changeAction.getUserQualifier());
            assertNull(changeAction.getError());
            assertTrue(changeAction.getPath().contains("/recovery"));
            assertEquals("POST", changeAction.getMetadata().getHttpMethod());

        }
    }

    //
//        @Test
//        @SneakyThrows
//        @DisplayName("Sends email message")
//        void sendsEmailMessage() {
//            var participant = participantCreator.givenParticipantExists(p -> {
//            });
//            var contact = givenContactExists(c -> {
//                c.setContactType(EMAIL);
//                c.setValue(participant.getUser().getLogin());
//                c.setUserInfo(participant.getUser().getUserInfo());
//            });
//            var user = participant.getUser();
//            mockMvc.perform(postRequest("/recovery?login=" + user.getLogin()))
//                    .andExpect(status().isOk())
//                    .andExpect(content().string(""));
//            assertThatEmailQueueContainsMessage(recoveryRenderDTO(participant), 1);
//        }
//
//        private RecoveryRenderDTO recoveryRenderDTO(Participant participant) {
//            RecoveryRenderDTO dto = new RecoveryRenderDTO();
//            final var confirmations = emailConfirmationRepository.findAllByUserAndType(participant.getUser(), CHANGE_PASSWORD);
//            assertEquals(1, confirmations.size());
//            dto.setRecovery(new Recovery(confirmations.get(0).getId()));
//            dto.setSubject(participant.getCommunity().getTitle() + ": restore password");
//            return withDefaultFieldsSet(dto, participant);
//        }
//
//        @Test
//        @SneakyThrows
//        @DisplayName("Creates additional email confirmation if already exists")
//        void createsAdditionalEmailConfirmation() {
//            var user = participantCreator.givenParticipantExists(p -> {
//            }).getUser();
//            var confirmation = givenEmailConfirmationExists(c -> {
//                c.setUser(user);
//            });
//            mockMvc.perform(postRequest("/recovery?login=" + user.getLogin()))
//                    .andExpect(status().isOk())
//                    .andExpect(content().string(""));
//            var confirmations = emailConfirmationRepository.findAllByUserAndType(user, CHANGE_PASSWORD);
//            assertFalse(confirmations.isEmpty());
//            assertEquals(2, confirmations.size());
//        }
//
//    }
//
//
    @Nested
    @DisplayName("On confirming password change")
    class OnPasswordChangeConfirmationRequest {

        @Test
        @SneakyThrows
        @DisplayName("Save api action")
        void saveApiAction() {
            final var newPassword = "new pass";
            final var confirmationId = UUID.randomUUID();
            var confirmationDto = new ChangePasswordController.ChangePasswordDTO();
            confirmationDto.setNewPassword(newPassword);
            mockMvc.perform(postRequest("/confirm-recovery?code=" + confirmationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(confirmationDto)))
                    .andExpect(status().isOk());
            await().atMost(2, SECONDS).until(() -> !apiActionRepository.findAllByUserQualifier(confirmationId.toString()).isEmpty());
            final var apiActions = apiActionRepository.findAllByUserQualifier(confirmationId.toString());
            assertEquals(1, apiActions.size());
            final var changeAction = apiActions.get(0);
            assertNull(changeAction.getError());
            assertTrue(changeAction.getPath().contains("/confirm-recovery"));
            assertEquals("POST", changeAction.getMetadata().getHttpMethod());
        }
    }
//        @Test
//        @SneakyThrows
//        @DisplayName("User type set to real even if it wasn't previously")
//        void userTypeSetToReal() {
//            var user = givenUserExists(u -> {
//                u.setUserType(GOOGLE);
//                u.setPassword("old password");
//            });
//            var confirmation = givenEmailConfirmationExists(c -> {
//                c.setUser(user);
//            });
//            final var newPassword = "new pass";
//            var confirmationDto = new ChangePasswordController.ChangePasswordDTO();
//            confirmationDto.setNewPassword(newPassword);
//            assertEquals(GOOGLE, user.getUserType());
//            mockMvc.perform(postRequest("/confirm-recovery?code=" + confirmation.getId())
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(new ObjectMapper().writeValueAsString(confirmationDto)))
//                    .andExpect(status().isOk())
//                    .andExpect(content().string(""));
//            final var updatedUser = userRepository.findById(user.getId()).orElseThrow();
//            assertEquals(REAL, updatedUser.getUserType());
//        }
//
//        @Test
//        @SneakyThrows
//        @DisplayName("Can't confirm password change with wrong code")
//        void doesntConfirmWithWrongCode() {
//            var user = givenUserExists(u -> {
//                u.setPassword("old password");
//            });
//            var confirmation = givenEmailConfirmationExists(c -> {
//                c.setUser(user);
//            });
//            final var newPassword = "new pass";
//            var confirmationDto = new ChangePasswordController.ChangePasswordDTO();
//            confirmationDto.setNewPassword(newPassword);
//            mockMvc.perform(postRequest("/confirm-recovery?code=" + UUID.randomUUID())
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(new ObjectMapper().writeValueAsString(confirmationDto)))
//                    .andExpect(status().isNotFound())
//                    .andExpect(content().string(""));
//            final var unchangedPassword = userRepository.findById(user.getId()).map(User::getPassword).orElseThrow();
//            assertEquals("old password", unchangedPassword);
//            assertFalse(emailConfirmationRepository.findAllByUserAndType(user, CHANGE_PASSWORD).isEmpty());
//        }
//
//    }

    @Nested
    @DisplayName("On direct password change")
    class OnDirectPasswordChange {

        @Test
        @SneakyThrows
        @DisplayName("Can't change password with null password provided")
        void cantChangeWithNull() {
            var user = givenUserExists(u -> u.setPassword("old password"));
            final var token = userAuthHeader(user);
            mockMvc.perform(postRequest("/password/change")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newPassword\": null}"))
                    .andExpect(status().isBadRequest());
            final var password = userRepository.findById(user.getId()).map(User::getPassword).orElseThrow();
            assertEquals("old password", password);
        }

        @Test
        @SneakyThrows
        @DisplayName("Can't change password with empty password provided")
        void cantChangeWithEmpty() {
            var user = givenUserExists(u -> u.setPassword("old password"));
            final var token = userAuthHeader(user);
            mockMvc.perform(postRequest("/password/change")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newPassword\": \"\"}"))
                    .andExpect(status().isBadRequest());
            final var password = userRepository.findById(user.getId()).map(User::getPassword).orElseThrow();
            assertEquals("old password", password);
        }

        @Test
        @SneakyThrows
        @DisplayName("Can't change password with missing password provided")
        void cantChangeWithMissingField() {
            var user = givenUserExists(u -> u.setPassword("old password"));
            final var token = userAuthHeader(user);
            mockMvc.perform(postRequest("/password/change")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
            final var password = userRepository.findById(user.getId()).map(User::getPassword).orElseThrow();
            assertEquals("old password", password);
        }

        @Test
        @SneakyThrows
        @DisplayName("Can change password when dto is proper")
        void canChangeWithProperDto() {
            var user = givenUserExists(u -> u.setPassword("old password"));
            final var token = userAuthHeader(user);
            final var newPassword = "new pass";
            mockMvc.perform(postRequest("/password/change")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newPassword\": \"" + newPassword + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));
            final var newEncodedPassword = userRepository.findById(user.getId()).map(User::getPassword).orElseThrow();
            assertNotEquals("old password", newEncodedPassword);
            assertTrue(passwordEncoder.matches(newPassword, newEncodedPassword));
        }

        @Test
        @SneakyThrows
        @DisplayName("Can't change password when not authorized")
        void cantChangeWhenNotAuthorized() {
            var user = givenUserExists(u -> u.setPassword("old password"));
            mockMvc.perform(postRequest("/password/change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newPassword\": \"new pass\"}"))
                    .andExpect(status().isForbidden());
            final var password = userRepository.findById(user.getId()).map(User::getPassword).orElseThrow();
            assertEquals("old password", password);
        }

        @Test
        @SneakyThrows
        @DisplayName("Saves api action")
        void savesApiAction() {
            var user = givenUserExists(u -> u.setPassword("old password"));
            final var token = userAuthHeader(user);
            final var newPassword = "new pass";
            mockMvc.perform(postRequest("/password/change")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newPassword\": \"" + newPassword + "\"}"))
                    .andExpect(status().isOk());
            // saving api action entity is async
            await().atMost(2, SECONDS).until(() -> !apiActionRepository.findAllByUserId(user.getId()).isEmpty());
            final var apiActions = apiActionRepository.findAllByUserId(user.getId());
            assertEquals(1, apiActions.size());
            final var changeAction = apiActions.get(0);
            assertEquals(user.getLogin(), changeAction.getUserQualifier());
            assertNull(changeAction.getError());
            assertTrue(changeAction.getPath().contains("/password/change"));
            assertEquals("POST", changeAction.getMetadata().getHttpMethod());
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
    protected String controllerPath() {
        return "/api/auth";
    }
}