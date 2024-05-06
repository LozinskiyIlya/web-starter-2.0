package com.starter.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.BillTag;
import com.starter.domain.repository.BillTagRepository;
import com.starter.domain.repository.testdata.BillTestDataCreator;
import com.starter.web.AbstractSpringIntegrationTest;
import com.starter.web.configuration.openai.AssistantProperties;
import com.starter.web.dto.BillDto;
import jakarta.transaction.Transactional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BillControllerIT extends AbstractSpringIntegrationTest {


    @Autowired
    private BillTestDataCreator billTestDataCreator;

    @Autowired
    private AssistantProperties assistantProperties;

    @Nested
    @DisplayName("Get Bill")
    class GetBill {

        @SneakyThrows
        @Test
        @DisplayName("returns 403 without token")
        void returns403() {
            final var bill = billTestDataCreator.givenBillExists(b -> {
            });
            mockMvc.perform(getRequest("/" + bill.getId()))
                    .andExpect(status().isForbidden());
        }

        @SneakyThrows
        @Test
        @DisplayName("returns 404 if not found")
        void returns404() {
            final var token = testUserAuthHeader();
            mockMvc.perform(getRequest("/" + UUID.randomUUID())
                            .header(token.getFirst(), token.getSecond()))
                    .andExpect(status().isNotFound());
        }

        @SneakyThrows
        @Test
        @DisplayName("bill mapped properly")
        void billMappedProperly() {
            // given
            final var group = billTestDataCreator.givenGroupExists(g -> {
            });
            final var bill = billTestDataCreator.givenBillExists(b -> b.setGroup(group));
            final var token = testUserAuthHeader();

            // when
            final var response = mockMvc.perform(getRequest("/" + bill.getId())
                            .header(token.getFirst(), token.getSecond()))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            final var dto = mapper.readValue(response, BillDto.class);

            // then
            assertThat(dto.getId()).isEqualTo(bill.getId());
            assertThat(dto.getBuyer()).isEqualTo(bill.getBuyer());
            assertThat(dto.getSeller()).isEqualTo(bill.getSeller());
            assertThat(dto.getPurpose()).isEqualTo(bill.getPurpose());
            assertThat(dto.getAmount()).isEqualTo(bill.getAmount());
            assertThat(dto.getCurrency()).isEqualTo(bill.getCurrency());
            assertThat(dto.getStatus()).isEqualTo(bill.getStatus());
            assertThat(dto.getDate()).isEqualTo(bill.getMentionedDate().toString());
            assertThat(dto.getGroup().getTitle()).isEqualTo(group.getTitle());
            assertThat(dto.getGroup().getId()).isEqualTo(group.getId());
            assertThat(dto.getCreatedAt()).isNotEmpty();
            dto.getTags().forEach(tag -> assertThat(bill.getTags()).anyMatch(t -> t.getName().equals(tag.getName())));
        }
    }

    @Nested
    @DisplayName("Update Bill")
    class UpdateBill {

        @SneakyThrows
        @Test
        @DisplayName("returns 403 without token")
        void returns403() {
            final var bill = billTestDataCreator.givenBillExists(b -> {
            });
            mockMvc.perform(postRequest("/" + bill.getId()))
                    .andExpect(status().isForbidden());
        }

        @SneakyThrows
        @Test
        @DisplayName("returns 403 if not bill owner")
        void returns403IfNotOwner() {
            final var bill = billTestDataCreator.givenBillExists(b -> {
            });
            final var token = testUserAuthHeader(); // not bill owner
            mockMvc.perform(postRequest("/" + bill.getId())
                            .header(token.getFirst(), token.getSecond())
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @SneakyThrows
        @Test
        @DisplayName("returns 404 if not found")
        void returns404() {
            final var token = testUserAuthHeader();
            mockMvc.perform(postRequest("/" + UUID.randomUUID())
                            .header(token.getFirst(), token.getSecond())
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isNotFound());
        }

        @SneakyThrows
        @Transactional
        @Test
        @DisplayName("bill updated properly")
        void billUpdatedProperly() {
            // given
            final var defaultTag = ((BillTagRepository) billTestDataCreator.billTagRepository())
                    .findByNameAndTagType("Work", BillTag.TagType.DEFAULT)
                    .orElseThrow();
            final var bill = billTestDataCreator.givenBillExists(b -> b.setTags(Set.of(defaultTag)));
            final var newTag = billTestDataCreator.givenBillTagExists(t -> t.setUser(bill.getGroup().getOwner()));
            final var newPurpose = "New purpose" + UUID.randomUUID();
            final var postContent = readResource("requests/bill/bill_update.json")
                    .replaceAll("#BILL_ID#", bill.getId().toString())
                    .replaceAll("#NEW_PURPOSE#", newPurpose)
                    .replaceAll("#OLD_TAG_ID#", defaultTag.getId().toString())
                    .replaceAll("#NEW_TAG_ID#", newTag.getId().toString())
                    .replaceAll("#NEW_TAG_NAME#", newTag.getId().toString());
            // when
            final var token = userAuthHeader(bill.getGroup().getOwner());
            mockMvc.perform(postRequest("/" + bill.getId())
                            .header(token.getFirst(), token.getSecond())
                            .contentType("application/json")
                            .content(postContent))
                    .andExpect(status().isOk());
            // then
            final var updatedBill = billTestDataCreator.billRepository().findById(bill.getId()).orElseThrow();
            assertThat(updatedBill.getTags()).hasSize(2);
            assertThat(updatedBill.getTags()).contains(newTag, defaultTag);
            assertThat(updatedBill.getPurpose()).isEqualTo(newPurpose);
            assertThat(updatedBill.getStatus()).isEqualTo(Bill.BillStatus.CONFIRMED);
            assertThat(updatedBill.getBuyer()).isEqualTo(bill.getBuyer());
            assertThat(updatedBill.getGroup().getId()).isEqualTo(bill.getGroup().getId());
        }
    }

    @Nested
    @DisplayName("Skip Bill")
    class SkipBill {

        @SneakyThrows
        @Test
        @DisplayName("returns 403 without token")
        void returns403() {
            final var bill = billTestDataCreator.givenBillExists(b -> {
            });
            mockMvc.perform(deleteRequest("/" + bill.getId()))
                    .andExpect(status().isForbidden());
        }

        @SneakyThrows
        @Test
        @DisplayName("returns 403 if not bill owner")
        void returns403IfNotOwner() {
            final var bill = billTestDataCreator.givenBillExists(b -> {
            });
            final var token = testUserAuthHeader(); // not bill owner
            mockMvc.perform(deleteRequest("/" + bill.getId())
                            .header(token.getFirst(), token.getSecond()))
                    .andExpect(status().isForbidden());
        }

        @SneakyThrows
        @Test
        @DisplayName("returns 404 if not found")
        void returns404() {
            final var token = testUserAuthHeader();
            mockMvc.perform(deleteRequest("/" + UUID.randomUUID())
                            .header(token.getFirst(), token.getSecond()))
                    .andExpect(status().isNotFound());
        }

        @SneakyThrows
        @Test
        @DisplayName("bill skipped")
        void billSkipped() {
            // given
            final var bill = billTestDataCreator.givenBillExists(b -> {
            });
            // when
            final var token = userAuthHeader(bill.getGroup().getOwner());
            mockMvc.perform(deleteRequest("/" + bill.getId())
                            .header(token.getFirst(), token.getSecond()))
                    .andExpect(status().isOk());
            // then
            final var skippedBill = billTestDataCreator.billRepository().findById(bill.getId()).orElseThrow();
            assertThat(skippedBill.getStatus()).isEqualTo(Bill.BillStatus.SKIPPED);
        }
    }

    @Nested
    @DisplayName("Get Tags")
    class GetTags {

        @SneakyThrows
        @Test
        @DisplayName("returns 403 without token")
        void returns403() {
            mockMvc.perform(getRequest("/tags"))
                    .andExpect(status().isForbidden());
        }

        @SneakyThrows
        @Test
        @DisplayName("tags mapped properly")
        void tagsMappedProperly() {
            // given
            final var userTag = billTestDataCreator.givenBillTagExists(t -> {
            });
            final var token = userAuthHeader(userTag.getUser());
            // when
            final var response = mockMvc.perform(getRequest("/tags")
                            .header(token.getFirst(), token.getSecond()))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            final var dto = mapper.readValue(response, new TypeReference<List<BillDto.BillTagDto>>() {
            });
            // then
            assertThat(dto).hasSize(assistantProperties.getBillTags().length + 1);
            assertThat(dto).anyMatch(t -> t.getName().equals(userTag.getName()));
            Arrays.stream(assistantProperties.getBillTags()).forEach(tag -> assertThat(dto).anyMatch(t -> t.getName().equals(tag)));
        }
    }

    @Override
    protected String controllerPath() {
        return "/api/bills";
    }
}