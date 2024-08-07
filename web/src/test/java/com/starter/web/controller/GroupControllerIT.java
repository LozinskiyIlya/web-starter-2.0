package com.starter.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Group;
import com.starter.domain.repository.testdata.BillTestDataCreator;
import com.starter.domain.repository.testdata.UserTestDataCreator;
import com.starter.web.AbstractSpringIntegrationTest;
import com.starter.web.dto.BillDto;
import com.starter.web.dto.GroupDto;
import com.starter.web.service.bill.GroupService.InsightsDto;
import com.starter.web.service.openai.OpenAiAssistant;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static com.starter.domain.repository.testdata.TimeTestData.TODAY_INSTANT;
import static com.starter.domain.repository.testdata.TimeTestData.YESTERDAY_INSTANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GroupControllerIT extends AbstractSpringIntegrationTest {


    @Autowired
    private BillTestDataCreator billCreator;

    @Autowired
    private UserTestDataCreator userTestDataCreator;

    @SpyBean
    private OpenAiAssistant openAiAssistant;

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
        @DisplayName("is expected get group bills")
        void returnsExpectedGetGroupBillsResult() {
            final var auth = token.get();
            mockMvc.perform(getRequest("/" + group.get().getId() + "/bills")
                            .header(auth.getFirst(), auth.getSecond()))
                    .andExpect(expectedStatusGet.get());
        }

        @SneakyThrows
        @Test
        @DisplayName("is expected get group members")
        void returnsExpectedGetGroupMembersResult() {
            final var auth = token.get();
            mockMvc.perform(getRequest("/" + group.get().getId() + "/members")
                            .header(auth.getFirst(), auth.getSecond()))
                    .andExpect(expectedStatusGet.get());
        }

        @SneakyThrows
        @Test
        @DisplayName("is expected get all groups result")
        void returnsExpectedGetAllGroupsResult() {
            final var auth = token.get();
            mockMvc.perform(getRequest("")
                            .header(auth.getFirst(), auth.getSecond()))
                    .andExpect(expectedStatusGetAll.get());
        }


        @SneakyThrows
        @Test
        @DisplayName("is expected change group result")
        void returnsExpectedChangeGroupResult() {
            final var auth = token.get();
            final var currency = "IDR";
            final var id = group.get().getId();
            final var status = mockMvc.perform(postRequest("/" + id + "/currency")
                            .header(auth.getFirst(), auth.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currency\":\"" + currency + "\"}"))
                    .andExpect(expectedStatusPost.get()).andReturn().getResponse().getStatus();
            if (status == 200) {
                final var changed = billCreator.groupRepository().findById(id).orElseThrow();
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
            group = billCreator::givenGroupExists;
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
            group = billCreator::givenGroupExists;
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
            group = () -> billCreator.givenGroupExists(g -> {
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
            group = () -> billCreator.givenGroupExists(g -> {
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
            final var bill = billCreator.givenBillExists(b -> {
                b.setGroup(group);
                b.setMentionedDate(YESTERDAY_INSTANT);
            });
            billCreator.givenBillExists(skippedBill -> {
                skippedBill.setGroup(group);
                skippedBill.setStatus(Bill.BillStatus.SKIPPED);
                skippedBill.setMentionedDate(TODAY_INSTANT);
            });
            // when
            final var auth = token.get();
            final var response = mockMvc.perform(getRequest("/" + group.getId())
                            .header(auth.getFirst(), auth.getSecond()))
                    .andExpect(expectedStatusGet.get())
                    .andReturn().getResponse().getContentAsString();
            final var dto = mapper.readValue(response, GroupDto.class);
            // then
            assertThat(dto.getId()).isEqualTo(group.getId());
            assertThat(dto.getTitle()).isEqualTo(group.getTitle());
            assertThat(dto.getBillsCount()).isEqualTo(1); // skipped bill is not counted
            assertThat(dto.getMembersCount()).isEqualTo(1);
            final var lastBill = dto.getLastBill();
            assertThat(lastBill.getId()).isEqualTo(bill.getId()); // skipped bill is chronologically last, but not included
            assertThat(lastBill.getAmount()).isEqualTo(bill.getAmount());
            assertThat(lastBill.getCurrency()).isEqualTo(bill.getCurrency());
            assertThat(lastBill.getPurpose()).isEqualTo(bill.getPurpose());
            // and then skipped bill is not included
            final var billsResponse = mockMvc.perform(getRequest("/" + group.getId() + "/bills")
                            .header(auth.getFirst(), auth.getSecond()))
                    .andExpect(expectedStatusGet.get())
                    .andReturn().getResponse().getContentAsString();
            final var bills = mapper.readValue(billsResponse, new TypeReference<RestResponsePage<BillDto>>() {
            }).getContent();
            assertThat(bills).hasSize(1);
            assertThat(bills.get(0).getId()).isEqualTo(bill.getId());
        }

        @SneakyThrows
        @Test
        @DisplayName("and groups sorted by last bill")
        void groupsSortedByLastBill() {
            // given
            final var group = this.group.get();
            final var anotherGroup = billCreator.givenGroupExists(g -> g.setOwner(group.getOwner()));
            billCreator.givenBillExists(b -> {
                b.setGroup(anotherGroup);
                b.setMentionedDate(TODAY_INSTANT);
            });
            billCreator.givenBillExists(b -> {
                b.setGroup(group);
                b.setMentionedDate(YESTERDAY_INSTANT);
            });
            // when
            final var auth = token.get();
            final var response = mockMvc.perform(getRequest("")
                            .header(auth.getFirst(), auth.getSecond()))
                    .andExpect(expectedStatusGet.get())
                    .andReturn().getResponse().getContentAsString();
            // then
            final var groups = mapper.readValue(response, new TypeReference<RestResponsePage<GroupDto>>() {
            });
            assertEquals(2, groups.getContent().size());
            final var dates = groups.getContent()
                    .stream()
                    .map(GroupDto::getLastBill)
                    .map(GroupDto.GroupLastBillDto::getDate)
                    .map(Instant::parse)
                    .map(Instant::toEpochMilli).toList();
            isSortedDescending(dates);
        }

        @SneakyThrows
        @Test
        @DisplayName("and insights work")
        void insightsWork() {
            // given
            final var group = this.group.get();
            final var insightsReturned = "some insights";
            doReturn(insightsReturned)
                    .when(openAiAssistant).getInsights(Mockito.anyString());
            // when
            final var auth = token.get();
            final var response = mockMvc.perform(getRequest("/" + group.getId() + "/insights")
                            .header(auth.getFirst(), auth.getSecond()))
                    .andExpect(expectedStatusGet.get())
                    .andReturn().getResponse().getContentAsString();
            // then
            final var dto = mapper.readValue(response, InsightsDto.class);
            assertThat(dto.text()).isEqualTo(insightsReturned);
            // and saves to the group
            final var updated = billCreator.groupRepository().findById(group.getId()).orElseThrow();
            assertThat(updated.getInsights()).isEqualTo(insightsReturned);
            assertNotNull(updated.getInsightsUpdatedAt());
        }

        @SneakyThrows
        @Test
        @DisplayName("and returns cached insights")
        void returnsCachedInsights() {
            // given
            final var cachedInsights = "cached insights";
            final var group = this.group.get();
            group.setInsights(cachedInsights);
            group.setInsightsUpdatedAt(Instant.now());
            billCreator.groupRepository().save(group);
            doReturn("never called")
                    .when(openAiAssistant).getInsights(Mockito.anyString());
            // when
            final var auth = token.get();
            final var response = mockMvc.perform(getRequest("/" + group.getId() + "/insights")
                            .header(auth.getFirst(), auth.getSecond()))
                    .andExpect(expectedStatusGet.get())
                    .andReturn().getResponse().getContentAsString();
            // then
            final var dto = mapper.readValue(response, InsightsDto.class);
            assertThat(dto.text()).isEqualTo(cachedInsights);
            // and never calls assistant
            Mockito.verify(openAiAssistant, Mockito.never()).getInsights(Mockito.anyString());
            // and group is not updated
            final var notUpdated = billCreator.groupRepository().findById(group.getId()).orElseThrow();
            assertThat(notUpdated.getInsights()).isEqualTo(cachedInsights);
        }

        @SneakyThrows
        @Test
        @DisplayName("and forcefully updates insights")
        void forceUpdateInsights() {
            // given
            final var cachedInsights = "cached insights";
            final var forceUpdateInsights = "forcefully updated insights";
            final var group = this.group.get();
            group.setInsights(cachedInsights);
            group.setInsightsUpdatedAt(YESTERDAY_INSTANT);
            billCreator.groupRepository().save(group);
            doReturn(forceUpdateInsights)
                    .when(openAiAssistant).getInsights(Mockito.anyString());
            // when
            final var auth = token.get();
            final var response = mockMvc.perform(getRequest("/" + group.getId() + "/insights")
                            .header(auth.getFirst(), auth.getSecond())
                            .param("forceUpdate", "true"))
                    .andExpect(expectedStatusGet.get())
                    .andReturn().getResponse().getContentAsString();
            // then
            final var dto = mapper.readValue(response, InsightsDto.class);
            assertThat(dto.text()).isEqualTo(forceUpdateInsights);
            // and group is updated
            final var updated = billCreator.groupRepository().findById(group.getId()).orElseThrow();
            assertThat(updated.getInsights()).isEqualTo(forceUpdateInsights);
            assertThat(updated.getInsightsUpdatedAt()).isAfter(YESTERDAY_INSTANT);
        }
    }

    @Override
    protected String controllerPath() {
        return "/api/groups";
    }
}