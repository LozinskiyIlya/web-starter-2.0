package com.starter.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.starter.domain.repository.testdata.BillTestDataCreator;
import com.starter.web.AbstractSpringIntegrationTest;
import com.starter.web.configuration.openai.AssistantProperties;
import com.starter.web.dto.BillDto;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
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
            final var bill = billTestDataCreator.givenBillExists(b -> {
            });
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
            assertThat(dto.getMentionedDate()).isEqualTo(bill.getMentionedDate());
            dto.getTags().forEach(tag -> assertThat(bill.getTags()).anyMatch(t -> t.getName().equals(tag.getName())));
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