package com.starter.web.controller.user;

import com.starter.domain.entity.Role;
import com.starter.domain.entity.User;
import com.starter.domain.entity.UserInfo;
import com.starter.domain.repository.Repository;
import com.starter.domain.repository.RoleRepository;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.domain.repository.UserRepository;
import com.starter.domain.repository.testdata.UserInfoTestData;
import com.starter.domain.repository.testdata.UserTestData;
import com.starter.web.AbstractSpringIntegrationTest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeleteUserControllerIT extends AbstractSpringIntegrationTest implements UserTestData {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    protected String controllerPath() {
        return "/api/users/delete";
    }

    private static String bodyWithPassword(String password) {
        return "{\"password\":\"" + password + "\"}";
    }

    @RequiredArgsConstructor
    abstract class AsUserWithSomeRole {

        protected final Supplier<User> userSupplier;

        @Test
        @SneakyThrows
        @DisplayName("Can't call without token")
        void cantCallWithoutToken() {
            var user = userSupplier.get();
            mockMvc.perform(deleteRequest("/" + user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithPassword("password")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @SneakyThrows
        @DisplayName("Can't call without body")
        void cantCallWithoutBody() {
            var user = userSupplier.get();
            var authToken = userAuthHeader(user);
            mockMvc.perform(deleteRequest("/" + user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(authToken.getFirst(), authToken.getSecond()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @SneakyThrows
        @DisplayName("Can call for self-delete when body and token are correct")
        void canSelfCallAsUserIfTokenAndPasswordAreCorrect() {
            var user = userSupplier.get();
            var header = userAuthHeader(user);
            mockMvc.perform(deleteRequest("/" + user.getId())
                            .header(header.getFirst(), header.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithPassword("password")))
                    .andExpect(status().isOk());
        }

        @Test
        @SneakyThrows
        @DisplayName("Tombstone is set")
        void tombstoneIsSet() {
            var user = userSupplier.get();
            var header = userAuthHeader(user);
            mockMvc.perform(deleteRequest("/" + user.getId())
                            .header(header.getFirst(), header.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithPassword("password")))
                    .andExpect(status().isOk());
            final var params = new MapSqlParameterSource()
                    .addValue("id", user.getId());
            var loginWithTombstone = jdbcTemplate.queryForObject("select login from users where id=:id", params, String.class);
            assertTrue(loginWithTombstone.matches("^" + user.getLogin() + "\\[deleted:\\d{4}-\\d{1,2}-\\d{1,2}T.+\\]$"));
        }

    }

    @Nested
    @DisplayName("As REAL user with user role")
    class AsRealUserWithUserRole extends AsUserWithSomeRole {

        public AsRealUserWithUserRole() {
            super(() -> givenUserExists(u -> {
                u.setUserType(User.UserType.REAL);
                u.setPassword(passwordEncoder.encode("password"));
            }));
        }

        @Test
        @SneakyThrows
        @DisplayName("Can't call without password in body")
        void cantCallWithoutPasswordInBody() {
            var user = userSupplier.get();
            var authToken = userAuthHeader(user);
            mockMvc.perform(deleteRequest("/" + user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .header(authToken.getFirst(), authToken.getSecond()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @SneakyThrows
        @DisplayName("Can't call for self-delete if password is incorrect")
        void cantSelfCallAsUserIfPasswordIsIncorrect() {
            var user = userSupplier.get();
            var header = userAuthHeader(user);
            mockMvc.perform(deleteRequest("/" + user.getId())
                            .header(header.getFirst(), header.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithPassword("wrong password")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @SneakyThrows
        @DisplayName("Can't call for other user deletion with own password")
        void cantCallForOtherUserAsUser() {
            var user = givenUserExists(u -> u.setPassword(passwordEncoder.encode("password")));
            var userToDelete = givenUserExists(u -> u.setPassword(passwordEncoder.encode("other user password")));
            var header = userAuthHeader(user);
            mockMvc.perform(deleteRequest("/" + userToDelete.getId())
                            .header(header.getFirst(), header.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithPassword("password")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @SneakyThrows
        @DisplayName("Can't call for other user with their password")
        void cantCallForOtherUserWithTheirPassword() {
            var user = userSupplier.get();
            var userToDelete = givenUserExists(u -> u.setPassword(passwordEncoder.encode("other user password")));
            var header = userAuthHeader(user);
            mockMvc.perform(deleteRequest("/" + userToDelete.getId())
                            .header(header.getFirst(), header.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithPassword("other user password")))
                    .andExpect(status().isForbidden());
        }

    }

    @Nested
    @DisplayName("As GOOGLE user with user role")
    class AsGoogleUserWithUserRole extends AsUserWithSomeRole {

        public AsGoogleUserWithUserRole() {
            super(() -> givenUserExists(u -> {
                u.setUserType(User.UserType.GOOGLE);
                u.setPassword(passwordEncoder.encode("password"));
            }));
        }

        @Test
        @SneakyThrows
        @DisplayName("Can call without password in body")
        void canCallWithoutPasswordInBody() {
            var user = userSupplier.get();
            var authToken = userAuthHeader(user);
            mockMvc.perform(deleteRequest("/" + user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .header(authToken.getFirst(), authToken.getSecond()))
                    .andExpect(status().isOk());
            final var params = new MapSqlParameterSource()
                    .addValue("id", user.getId());
            var state = jdbcTemplate.queryForObject("select state from users where id=:id", params, String.class);
            assertEquals("DELETED", state);
        }

        @Test
        @SneakyThrows
        @DisplayName("Can call for self-delete if password is incorrect")
        void canSelfCallAsUserIfPasswordIsIncorrect() {
            var user = userSupplier.get();
            var header = userAuthHeader(user);
            mockMvc.perform(deleteRequest("/" + user.getId())
                            .header(header.getFirst(), header.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithPassword("wrong password")))
                    .andExpect(status().isOk());
            final var params = new MapSqlParameterSource()
                    .addValue("id", user.getId());
            var state = jdbcTemplate.queryForObject("select state from users where id=:id", params, String.class);
            assertEquals("DELETED", state);
        }

        @Test
        @SneakyThrows
        @DisplayName("Can't call for other user deletion with own password")
        void cantCallForOtherUserAsUser() {
            var user = givenUserExists(u -> u.setPassword(passwordEncoder.encode("password")));
            var userToDelete = givenUserExists(u -> u.setPassword(passwordEncoder.encode("other user password")));
            var header = userAuthHeader(user);
            mockMvc.perform(deleteRequest("/" + userToDelete.getId()).header(header.getFirst(), header.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithPassword("password")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @SneakyThrows
        @DisplayName("Can't call for other user with their password")
        void cantCallForOtherUserWithTheirPassword() {
            var user = userSupplier.get();
            var userToDelete = givenUserExists(u -> u.setPassword(passwordEncoder.encode("other user password")));
            var header = userAuthHeader(user);
            mockMvc.perform(deleteRequest("/" + userToDelete.getId()).header(header.getFirst(), header.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithPassword("other user password")))
                    .andExpect(status().isForbidden());
        }

    }

    @Nested
    @DisplayName("As user with admin role")
    class AsUserWithAdminRole extends AsUserWithSomeRole {

        public AsUserWithAdminRole() {
            super(() -> givenUserWithAdminRoleExists(u -> u.setPassword(passwordEncoder.encode("password"))));
        }

        @Test
        @SneakyThrows
        @DisplayName("Can call for other user with own password")
        void canSelfCallAsAdmin() {
            var admin = givenUserWithAdminRoleExists(u -> u.setPassword(passwordEncoder.encode("password")));
            var otherUser = givenUserExists(u -> u.setPassword(passwordEncoder.encode("other password")));
            var header = userAuthHeader(admin);
            mockMvc.perform(deleteRequest("/" + otherUser.getId()).header(header.getFirst(), header.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithPassword("password")))
                    .andExpect(status().isOk());
        }

        @Test
        @SneakyThrows
        @DisplayName("Can call for other user with their password")
        void cantCallForOtherUserWithTheirPassword() {
            var user = userSupplier.get();
            var userToDelete = givenUserExists(u -> u.setPassword(passwordEncoder.encode("other user password")));
            var header = userAuthHeader(user);
            mockMvc.perform(deleteRequest("/" + userToDelete.getId()).header(header.getFirst(), header.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithPassword("other user password")))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("On user deletion")
    @TestComponent
    class OnUserDeletion implements UserInfoTestData {

        @Autowired
        private UserInfoRepository userInfoRepository;

        @TestFactory
        @SneakyThrows
        @DisplayName("All related entities are deleted")
        Stream<DynamicTest> relatedEntitiesAreDeleted() {
            var user = givenUserExists(u -> u.setPassword(passwordEncoder.encode("password")));
            var userInfo = givenUserInfoExists(ui -> ui.setUser(user));
            //when then
            var header = userAuthHeader(user);
            mockMvc.perform(deleteRequest("/" + user.getId())
                            .header(header.getFirst(), header.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithPassword("password")))
                    .andExpect(status().isOk());

            return Stream.<Pair<String, Runnable>>of(
                            Pair.of("user", () -> assertFalse(userRepository.existsById(user.getId()))),
                            Pair.of("userInfo", () -> assertFalse(userInfoRepository.existsById(userInfo.getId())))
                            )
                    .map(it -> DynamicTest.dynamicTest(it.getFirst(), it.getSecond()::run));
        }


        @Override
        public Repository<UserInfo> userInfoRepository() {
            return userInfoRepository;
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
}
