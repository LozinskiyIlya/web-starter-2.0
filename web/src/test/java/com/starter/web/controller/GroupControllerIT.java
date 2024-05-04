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
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static com.starter.common.service.JwtProvider.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GroupControllerIT extends AbstractSpringIntegrationTest {


    @Autowired
    private BillTestDataCreator billTestDataCreator;

    @Autowired
    private UserTestDataCreator userTestDataCreator;


    @RequiredArgsConstructor
    abstract class GetGroup {
        protected Supplier<Group> group;
        protected Supplier<String> token;
        protected Supplier<ResultMatcher> expectedStatus;

        @SneakyThrows
        @Test
        @DisplayName("is expected GET result")
        void returnsExpectedResult() {
            mockMvc.perform(getRequest("/" + group.get().getId())
                            .header(AUTHORIZATION, token.get()))
                    .andExpect(expectedStatus.get());
        }
    }

    @Nested
    @DisplayName("For non-existing group 404")
    public class ForNonExistingGroup extends GetGroup {
        {
            final var notPersisted = new Group();
            notPersisted.setId(UUID.randomUUID());
            group = () -> notPersisted;
            token = testUserAuthHeader()::getSecond;
            expectedStatus = status()::isNotFound;
        }
    }

    @Nested
    @DisplayName("As non-existing user 403")
    public class AsNonExistingUser extends GetGroup {
        {
            group = billTestDataCreator::givenGroupExists;
            token = UUID.randomUUID()::toString;
            expectedStatus = status()::isForbidden;
        }
    }

    @Nested
    @DisplayName("As some user 403")
    public class AsSomeUser extends GetGroup {
        {
            group = billTestDataCreator::givenGroupExists;
            token = testUserAuthHeader()::getSecond;
            expectedStatus = status()::isForbidden;
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
            token = () -> userAuthHeader(owner).getSecond();
            expectedStatus = status()::is2xxSuccessful;
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
            token = () -> userAuthHeader(owner).getSecond();
            expectedStatus = status()::is2xxSuccessful;
        }

        @SneakyThrows
        @Test
        @DisplayName("and group mapped properly")
        void groupMappedProperly() {
            // given
            final var group = this.group.get();
            final var bill = billTestDataCreator.givenBillExists(b -> b.setGroup(group));
            // when
            final var response = mockMvc.perform(getRequest("/" + group.getId())
                            .header(AUTHORIZATION, token.get()))
                    .andExpect(expectedStatus.get())
                    .andReturn().getResponse().getContentAsString();
            final var dto = mapper.readValue(response, GroupDto.class);
            // then
            assertThat(dto.getId()).isEqualTo(group.getId());
            assertThat(dto.getTitle()).isEqualTo(group.getTitle());
            assertThat(dto.getBills()).hasSize(1);
            final var billDto = dto.getBills().get(0);
            assertThat(billDto.getId()).isEqualTo(bill.getId());
        }
    }

    @Override
    protected String controllerPath() {
        return "/api/groups";
    }
}