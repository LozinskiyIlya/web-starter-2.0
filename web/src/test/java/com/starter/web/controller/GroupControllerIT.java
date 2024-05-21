package com.starter.web.controller;

import com.starter.domain.entity.Group;
import com.starter.domain.repository.testdata.BillTestDataCreator;
import com.starter.domain.repository.testdata.UserTestDataCreator;
import com.starter.web.AbstractSpringIntegrationTest;
import com.starter.web.dto.GroupDto;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GroupControllerIT extends AbstractSpringIntegrationTest {


    @Autowired
    private BillTestDataCreator billTestDataCreator;

    @Autowired
    private UserTestDataCreator userTestDataCreator;

    @RequiredArgsConstructor
    abstract class GetGroup {
        protected Supplier<Group> group;
        protected Supplier<Pair<String, String>> token;
        protected Supplier<ResultMatcher> expectedStatusGet;
        protected Supplier<ResultMatcher> expectedStatusGetAll;
        protected Supplier<ResultMatcher> expectedStatusPost;


        @SneakyThrows
        @Test
        @DisplayName("is expected get by id result")
        void returnsExpectedGetByIdResult() {
            final var auth = token.get();
            mockMvc.perform(getRequest("/" + group.get().getId())
                            .header(auth.getFirst(), auth.getSecond()))
                    .andExpect(expectedStatusGet.get());
        }

        @SneakyThrows
        @Test
        @DisplayName("is expected get all groups result")
        void returnsExpectedGetAllGroupsResult() {
            final var auth = token.get();
            final var result = mockMvc.perform(getRequest("")
                            .header(auth.getFirst(), auth.getSecond()))
                    .andExpect(expectedStatusGetAll.get());
            if (expectedStatusGet.get().equals(status().is2xxSuccessful())) {
                final var response = result.andReturn().getResponse().getContentAsString();
                final var groups = mapper.readValue(response, GroupDto[].class);
                assertThat(groups).hasSize(1);
            }
        }


        @SneakyThrows
        @Test
        @DisplayName("is expected change group result")
        void returnsExpectedChangeGroupResult() {
            final var auth = token.get();
            final var currency = "IDR";
            mockMvc.perform(postRequest("/" + group.get().getId() + "/currency")
                            .header(auth.getFirst(), auth.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currency\":\"" + currency + "\"}"))
                    .andExpect(expectedStatusPost.get());
            if (expectedStatusPost.get().equals(status().is2xxSuccessful())) {
                final var changed = billTestDataCreator.groupRepository().findById(group.get().getId()).orElseThrow();
                assertEquals(currency, changed.getDefaultCurrency());
            }
        }
    }

    @Nested
    @DisplayName("For non-existing group 404")
    public class ForNonExistingGroup extends GetGroup {
        {
            final var notPersisted = new Group();
            notPersisted.setId(UUID.randomUUID());
            group = () -> notPersisted;
            token = GroupControllerIT.this::testUserAuthHeader;
            expectedStatusGet = status()::isNotFound;
            expectedStatusPost = expectedStatusGet;
            expectedStatusGetAll = status()::is2xxSuccessful;
        }
    }

    @Nested
    @DisplayName("As non-existing user 403")
    public class AsNonExistingUser extends GetGroup {
        {
            group = billTestDataCreator::givenGroupExists;
            token = GroupControllerIT.this::userAuthHeaderUnchecked;
            expectedStatusGet = status()::isForbidden;
            expectedStatusPost = expectedStatusGet;
            expectedStatusGetAll = expectedStatusGet;
        }
    }

    @Nested
    @DisplayName("As some user 403")
    public class AsSomeUser extends GetGroup {
        {
            group = billTestDataCreator::givenGroupExists;
            token = GroupControllerIT.this::testUserAuthHeader;
            expectedStatusGet = status()::isForbidden;
            expectedStatusPost = expectedStatusGet;
            expectedStatusGetAll = status()::is2xxSuccessful;
        }
    }

    @Nested
    @DisplayName("As group member 200")
    public class AsGroupMember extends GetGroup {
        {
            final var owner = userTestDataCreator.givenUserInfoExists(ui -> {
            }).getUser();
            final var member = userTestDataCreator.givenUserInfoExists(ui -> {
            }).getUser();
            group = () -> billTestDataCreator.givenGroupExists(g -> {
                g.setOwner(owner);
                g.setMembers(List.of(owner, member));
            });
            token = () -> userAuthHeader(member);
            expectedStatusGet = status()::is2xxSuccessful;
            expectedStatusPost = status()::isForbidden;
            expectedStatusGetAll = expectedStatusGet;
        }
    }

    @Nested
    @DisplayName("As group owner 200")
    public class AsGroupOwner extends GetGroup {
        {
            final var owner = userTestDataCreator.givenUserInfoExists(ui -> {
            }).getUser();
            group = () -> billTestDataCreator.givenGroupExists(g -> {
                g.setOwner(owner);
                g.setMembers(List.of(owner));
            });
            token = () -> userAuthHeader(owner);
            expectedStatusGet = status()::is2xxSuccessful;
            expectedStatusPost = expectedStatusGet;
            expectedStatusGetAll = expectedStatusGet;
        }

        @SneakyThrows
        @Test
        @DisplayName("and group mapped properly")
        void groupMappedProperly() {
            // given
            final var group = this.group.get();
            final var auth = token.get();
            final var bill = billTestDataCreator.givenBillExists(b -> b.setGroup(group));
            // when
            final var response = mockMvc.perform(getRequest("/" + group.getId())
                            .header(auth.getFirst(), auth.getSecond()))
                    .andExpect(expectedStatusGet.get())
                    .andReturn().getResponse().getContentAsString();
            final var dto = mapper.readValue(response, GroupDto.class);
            // then
            assertThat(dto.getId()).isEqualTo(group.getId());
            assertThat(dto.getTitle()).isEqualTo(group.getTitle());
            assertThat(dto.getBillsCount()).isEqualTo(1);
            assertThat(dto.getMembersCount()).isEqualTo(1);
            final var lastBill = dto.getLastBill();
            assertThat(lastBill).isNotNull();
            assertThat(lastBill.getId()).isEqualTo(bill.getId());
            assertThat(lastBill.getAmount()).isEqualTo(bill.getAmount());
            assertThat(lastBill.getCurrency()).isEqualTo(bill.getCurrency());
            assertThat(lastBill.getPurpose()).isEqualTo(bill.getPurpose());
        }
    }

    @Override
    protected String controllerPath() {
        return "/api/groups";
    }
}