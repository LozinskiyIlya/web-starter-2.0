package com.starter.web.controller;

import com.starter.domain.repository.testdata.BillTestDataCreator;
import com.starter.web.AbstractSpringIntegrationTest;
import com.starter.web.dto.GroupDto;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GroupControllerIT extends AbstractSpringIntegrationTest {


    @Autowired
    private BillTestDataCreator billTestDataCreator;

    @Nested
    @DisplayName("Get Group")
    class GetGroup {

        @SneakyThrows
        @Test
        @DisplayName("returns 403 without token")
        void returns403() {
            final var bill = billTestDataCreator.givenBillExists(b -> {
            });
            mockMvc.perform(getRequest("/" + bill.getGroup().getId()))
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
        @DisplayName("group mapped properly")
        void groupMappedProperly() {
            // given
            final var group = billTestDataCreator.givenGroupExists(g -> {
            });
            final var bill = billTestDataCreator.givenBillExists(b -> b.setGroup(group));
            final var token = testUserAuthHeader();
            // when
            final var response = mockMvc.perform(getRequest("/" + group.getId())
                            .header(token.getFirst(), token.getSecond()))
                    .andExpect(status().isOk())
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