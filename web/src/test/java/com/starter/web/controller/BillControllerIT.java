package com.starter.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.BillTag;
import com.starter.domain.repository.BillTagRepository;
import com.starter.domain.repository.testdata.BillTestDataCreator;
import com.starter.domain.repository.testdata.UserTestDataCreator;
import com.starter.web.AbstractSpringIntegrationTest;
import com.starter.web.configuration.openai.AssistantProperties;
import com.starter.web.dto.BillDto;
import com.starter.web.fragments.RecognitionRequest;
import com.starter.web.service.openai.OpenAiAssistant;
import jakarta.transaction.Transactional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.starter.web.controller.BillController.MAX_CUSTOM_TAGS_PER_USER;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BillControllerIT extends AbstractSpringIntegrationTest {

    @Autowired
    private BillTestDataCreator billCreator;

    @Autowired
    private UserTestDataCreator userCreator;

    @Autowired
    private AssistantProperties assistantProperties;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @SpyBean
    private OpenAiAssistant openAiAssistant;

    @Nested
    @DisplayName("Get Bill")
    class GetBill {

        @SneakyThrows
        @Test
        @DisplayName("returns 403 without token")
        void returns403() {
            final var bill = billCreator.givenBillExists(b -> {
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
            final var group = billCreator.givenGroupExists(g -> {
            });
            final var bill = billCreator.givenBillExists(b -> b.setGroup(group));
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
            final var bill = billCreator.givenBillExists(b -> {
            });
            mockMvc.perform(postRequest("/" + bill.getId()))
                    .andExpect(status().isForbidden());
        }

        @SneakyThrows
        @Test
        @DisplayName("returns 403 if not bill owner")
        void returns403IfNotOwner() {
            final var bill = billCreator.givenBillExists(b -> {
            });
            final var token = testUserAuthHeader(); // not bill owner
            mockMvc.perform(postRequest("/" + bill.getId())
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(MIN_FIELDS_DTO.formatted(bill.getGroup().getId())))
                    .andExpect(status().isForbidden());
        }

        @SneakyThrows
        @Test
        @DisplayName("returns 404 if not found")
        void returns404() {
            final var token = testUserAuthHeader();
            mockMvc.perform(postRequest("/" + UUID.randomUUID())
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(MIN_FIELDS_DTO.formatted(UUID.randomUUID())))
                    .andExpect(status().isNotFound());
        }

        @SneakyThrows
        @Transactional
        @Test
        @DisplayName("bill updated properly")
        void billUpdatedProperly() {
            // given
            final var defaultTag = ((BillTagRepository) billCreator.billTagRepository())
                    .findByNameAndTagType("Work", BillTag.TagType.DEFAULT)
                    .orElseThrow();
            final var bill = billCreator.givenBillExists(b -> b.setTags(Set.of(defaultTag)));
            final var newTag = billCreator.givenBillTagExists(t -> t.setUser(bill.getGroup().getOwner()));
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
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(postContent))
                    .andExpect(status().isOk());
            // then
            final var updatedBill = billCreator.billRepository().findById(bill.getId()).orElseThrow();
            assertThat(updatedBill.getTags()).hasSize(2);
            assertThat(updatedBill.getTags()).contains(newTag, defaultTag);
            assertThat(updatedBill.getPurpose()).isEqualTo(newPurpose);
            assertThat(updatedBill.getBuyer()).isEqualTo(bill.getBuyer());
            assertThat(updatedBill.getGroup().getId()).isEqualTo(bill.getGroup().getId());
        }
    }

    @Nested
    @DisplayName("Add Bill")
    class AddBill {

        @SneakyThrows
        @Test
        @DisplayName("returns 403 without token")
        void returns403() {
            mockMvc.perform(postRequest(""))
                    .andExpect(status().isForbidden());
        }

        @SneakyThrows
        @Test
        @DisplayName("returns 403 if not group owner")
        void returns403IfNotOwner() {
            final var group = billCreator.givenGroupExists(b -> {
            });
            final var token = testUserAuthHeader(); // not group owner
            mockMvc.perform(postRequest("")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(MIN_FIELDS_DTO.formatted(group.getId())))
                    .andExpect(status().isForbidden());
        }

        @SneakyThrows
        @Test
        @DisplayName("returns 404 if group not found")
        void returns404() {
            final var token = testUserAuthHeader();
            mockMvc.perform(postRequest("/" + UUID.randomUUID())
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(MIN_FIELDS_DTO.formatted(UUID.randomUUID())))
                    .andExpect(status().isNotFound());
        }

        @SneakyThrows
        @Transactional
        @Test
        @DisplayName("bill added properly")
        void billAddedProperly() {
            // given
            final var group = billCreator.givenGroupExists(b -> {
            });
            final var workTag = ((BillTagRepository) billCreator.billTagRepository())
                    .findByNameAndTagType("Work", BillTag.TagType.DEFAULT)
                    .orElseThrow();
            final var newPurpose = "New purpose" + UUID.randomUUID();
            final var postContent = readResource("requests/bill/bill_new.json")
                    .replaceAll("#PURPOSE#", newPurpose)
                    .replaceAll("#GROUP_ID#", group.getId().toString())
                    .replaceAll("#TAG_ID#", workTag.getId().toString());
            // when
            final var token = userAuthHeader(group.getOwner());
            final var response = mockMvc.perform(postRequest("")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(postContent))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            final var createdBillId = mapper.readValue(response, UUID.class);
            // then
            final var createdBill = billCreator.billRepository().findById(createdBillId).orElseThrow();
            assertThat(createdBill.getTags()).hasSize(1);
            assertThat(createdBill.getTags()).contains(workTag);
            assertThat(createdBill.getPurpose()).isEqualTo(newPurpose);
            assertThat(createdBill.getGroup().getId()).isEqualTo(group.getId());
        }
    }

    @Nested
    @DisplayName("Parse Bill")
    class ParseBill {

        @SneakyThrows
        @Test
        @DisplayName("returns 403 without token")
        void returns403() {
            mockMvc.perform(postRequest("/parse"))
                    .andExpect(status().isForbidden());
        }

        @SneakyThrows
        @Test
        @DisplayName("returns 403 if not group owner")
        void returns403IfNotOwner() {
            final var group = billCreator.givenGroupExists(b -> {
            });
            final var dto = random.nextObject(RecognitionRequest.class);
            dto.setGroupId(group.getId());
            final var token = testUserAuthHeader(); // not group owner
            mockMvc.perform(postRequest("/parse")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());
        }

        @SneakyThrows
        @Test
        @DisplayName("returns 404 if group not found")
        void returns404() {
            final var token = testUserAuthHeader();
            final var dto = random.nextObject(RecognitionRequest.class);
            mockMvc.perform(postRequest("/parse")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }

        @SneakyThrows
        @Transactional
        @Test
        @DisplayName("bill parsed properly from text")
        void textParsedProperly() {
            // given
            final var group = billCreator.givenGroupExists();
            final var dto = new RecognitionRequest();
            dto.setType(RecognitionRequest.RecognitionType.TEXT);
            dto.setGroupId(group.getId());
            dto.setDetails("some bill details");
            doReturn(assistantResponse("USD", 100.0))
                    .when(openAiAssistant).runTextPipeline(Mockito.eq(dto.getDetails()), Mockito.any(), Mockito.anySet());
            // when
            final var token = userAuthHeader(group.getOwner());
            final var response = mockMvc.perform(postRequest("/parse")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            final var createdBillId = mapper.readValue(response, UUID.class);
            // then
            final var createdBill = billCreator.billRepository().findById(createdBillId).orElseThrow();
            assertThat(createdBill.getCurrency()).isEqualTo("USD");
            assertThat(createdBill.getAmount()).isEqualTo(100.0);
        }

        @SneakyThrows
        @Test
        @DisplayName("bill parsed properly from image")
        void imageParsedProperly() {
            // given
            final var group = billCreator.givenGroupExists();
            final var dto = new RecognitionRequest();
            dto.setType(RecognitionRequest.RecognitionType.IMAGE);
            dto.setGroupId(group.getId());
            dto.setDetails("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAB0lEQVR42mP8/wcAAwAB/IDx4XwAAAAASUVORK5CYII=");
            dto.setFileName("image.png");
            doReturn(assistantResponse("USD", 100.0))
                    .when(openAiAssistant).runFilePipeline(Mockito.eq(dto.getDetails()), Mockito.anyString(), Mockito.any(), Mockito.anySet());
            // when
            final var token = userAuthHeader(group.getOwner());
            final var response = mockMvc.perform(postRequest("/parse")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            final var createdBillId = mapper.readValue(response, UUID.class);
            // then
            // attachment uploading is async
            await().atMost(10, TimeUnit.SECONDS)
                    .until(() -> billCreator.billRepository().findById(createdBillId).filter(b -> b.getAttachment() != null).isPresent());
            final var createdBill = billCreator.billRepository().findById(createdBillId).orElseThrow();
            assertThat(createdBill.getCurrency()).isEqualTo("USD");
            assertThat(createdBill.getAmount()).isEqualTo(100.0);
            assertTrue(createdBill.getAttachment().toString().contains(createdBill.getId().toString()));
        }

        @SneakyThrows
        @Transactional
        @Test
        @DisplayName("select Personal group if groupId not present")
        void selectPersonal() {
            // given
            final var chatId = random.nextLong();
            final var user = userCreator.givenUserInfoExists(ui -> ui.setTelegramChatId(chatId)).getUser();
            final var personal = billCreator.givenGroupExists(g -> {
                g.setOwner(user);
                g.setChatId(chatId);
            });
            final var dto = new RecognitionRequest();
            dto.setType(RecognitionRequest.RecognitionType.TEXT);
            dto.setDetails("some bill details");
            doReturn(assistantResponse("USD", 100.0))
                    .when(openAiAssistant).runTextPipeline(Mockito.eq(dto.getDetails()), Mockito.any(), Mockito.anySet());
            // when
            final var token = userAuthHeader(user);
            final var response = mockMvc.perform(postRequest("/parse")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            final var createdBillId = mapper.readValue(response, UUID.class);
            // then
            final var createdBill = billCreator.billRepository().findById(createdBillId).orElseThrow();
            assertThat(createdBill.getGroup().getId()).isEqualTo(personal.getId());
        }
    }

    @Nested
    @DisplayName("Skip Bill")
    class SkipBill {

        @SneakyThrows
        @Test
        @DisplayName("returns 403 without token")
        void returns403() {
            final var bill = billCreator.givenBillExists(b -> {
            });
            mockMvc.perform(deleteRequest("/" + bill.getId()))
                    .andExpect(status().isForbidden());
            mockMvc.perform(deleteRequest(""))
                    .andExpect(status().isForbidden());
        }

        @SneakyThrows
        @Test
        @DisplayName("returns 403 if not bill owner")
        void returns403IfNotOwner() {
            final var bill = billCreator.givenBillExists(b -> {
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
            final var bill = billCreator.givenBillExists(b -> {
            });
            // when
            final var token = userAuthHeader(bill.getGroup().getOwner());
            mockMvc.perform(deleteRequest("/" + bill.getId())
                            .header(token.getFirst(), token.getSecond()))
                    .andExpect(status().isOk());
            // then
            final var skippedBill = billCreator.billRepository().findById(bill.getId()).orElseThrow();
            assertThat(skippedBill.getStatus()).isEqualTo(Bill.BillStatus.SKIPPED);
        }

        @SneakyThrows
        @Test
        @DisplayName("all bills skipped")
        void allBillSkipped() {
            // given
            final var bill = billCreator.givenBillExists(b -> {
            });
            final var anotherBill = billCreator.givenBillExists(b -> {
            });
            final var ids = List.of(bill.getId(), anotherBill.getId());
            // when
            final var token = userAuthHeader(bill.getGroup().getOwner());
            mockMvc.perform(deleteRequest("")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(ids)))
                    .andExpect(status().isOk());
            // then
            billCreator.billRepository().findAllById(ids)
                    .forEach(b -> assertThat(b.getStatus()).isEqualTo(Bill.BillStatus.SKIPPED));
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
            final var userTag = billCreator.givenBillTagExists(t -> {
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

    @Nested
    @DisplayName("Create Tag")
    class CreateTag {

        @Test
        @DisplayName("returns 400 if name already exists")
        void returns400IfNameExists() throws Exception {
            // given
            final var user = userCreator.givenSubscriptionExists(s -> {
            }).getUser();
            final var existingName = billCreator.givenBillTagExists(t -> {
                t.setName(UUID.randomUUID().toString());
                t.setUser(user);
            }).getName();
            final var allTags = ((BillTagRepository) billCreator.billTagRepository()).findAllByUser(user);
            assertTrue(allTags.stream().anyMatch(t -> t.getName().equals(existingName)));
            final var token = userAuthHeader(user);
            final var dto = userDefinedTagDto();
            dto.setName(existingName);
            // when then
            mockMvc.perform(postRequest("/tags")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 403 if not premium")
        void returns403IfNotPremium() throws Exception {
            // given
            final var userWithoutPremium = userCreator.givenUserExists();
            final var token = userAuthHeader(userWithoutPremium);
            final var dto = userDefinedTagDto();
            // when then
            mockMvc.perform(postRequest("/tags")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 403 if premium expired")
        void returns403IfPremiumEnded() throws Exception {
            // given
            final var userWithExpiredPremium = userCreator.givenSubscriptionExists(s ->
                            s.setEndsAt(now().minusDays(1).toInstant(ZoneOffset.UTC)))
                    .getUser();
            final var token = userAuthHeader(userWithExpiredPremium);
            final var dto = userDefinedTagDto();
            // when then
            mockMvc.perform(postRequest("/tags")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());
        }


        @Test
        @DisplayName("max tag count checked")
        void maxTagCountChecked() throws Exception {
            // given
            final var user = userCreator.givenSubscriptionExists(s -> {
            }).getUser();
            for (int i = 0; i < MAX_CUSTOM_TAGS_PER_USER; i++) {
                billCreator.givenBillTagExists(t -> t.setUser(user));
            }
            final var token = userAuthHeader(user);
            final var dto = userDefinedTagDto();
            // when then
            mockMvc.perform(postRequest("/tags")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Invalid type")
        void invalidType() throws Exception {
            // given
            final var user = userCreator.givenSubscriptionExists(s -> {
            }).getUser();
            final var token = userAuthHeader(user);
            final var dto = userDefinedTagDto();
            dto.setTagType(BillTag.TagType.DEFAULT); // not allowed
            // when then
            mockMvc.perform(postRequest("/tags")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("tag created properly")
        void tagCreatedProperly() throws Exception {
            // given
            final var user = userCreator.givenSubscriptionExists(s -> {
            }).getUser();
            final var token = userAuthHeader(user);
            final var dto = userDefinedTagDto();
            // when
            final var response = mockMvc.perform(postRequest("/tags")
                            .header(token.getFirst(), token.getSecond())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            final var createdTagId = mapper.readValue(response, UUID.class);
            // then
            final var createdTag = billCreator.billTagRepository().findById(createdTagId).orElseThrow();
            assertThat(createdTag.getName()).isEqualTo(dto.getName());
            assertThat(createdTag.getHexColor()).isEqualTo(dto.getHexColor());
            assertThat(createdTag.getTagType()).isEqualTo(dto.getTagType());
        }

        private BillDto.BillTagDto userDefinedTagDto() {
            final var dto = random.nextObject(BillDto.BillTagDto.class);
            dto.setTagType(BillTag.TagType.USER_DEFINED);
            return dto;
        }
    }

    @Nested
    @DisplayName("Delete tag")
    class DeleteTag {

        @Test
        @DisplayName("returns 404 if tag not found")
        void returns404IfNotFound() throws Exception {
            final var token = testUserAuthHeader();
            mockMvc.perform(deleteRequest("/tags/" + UUID.randomUUID())
                            .header(token.getFirst(), token.getSecond()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 403 if trying to delete not own tag")
        void returns403IfNotOwnTaf() throws Exception {
            final var tag = billCreator.givenBillTagExists(t -> {
            });
            final var token = testUserAuthHeader();
            mockMvc.perform(deleteRequest("/tags/" + tag.getId())
                            .header(token.getFirst(), token.getSecond()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("tag deleted properly")
        void tagDeletedProperly() throws Exception {
            final var tag = billCreator.givenBillTagExists(t -> {
            });
            final var token = userAuthHeader(tag.getUser());
            mockMvc.perform(deleteRequest("/tags/" + tag.getId())
                            .header(token.getFirst(), token.getSecond()))
                    .andExpect(status().isOk());
            assertThat(billCreator.billTagRepository().findById(tag.getId())).isEmpty();
            // and tombstone is set
            final var params = new MapSqlParameterSource()
                    .addValue("id", tag.getId());
            var loginWithTombstone = jdbcTemplate.queryForObject("select name from bill_tags where id=:id", params, String.class);
            assertTrue(loginWithTombstone.matches("^" + tag.getName() + "\\[deleted:\\d{4}-\\d{1,2}-\\d{1,2}T.+\\]$"));
        }
    }

    private static final String MIN_FIELDS_DTO = "{\"group\": {\"id\" : \"%s\"}, \"purpose\": \"Purpose\", \"amount\": 100, \"currency\": \"USD\"}";

    @Override
    protected String controllerPath() {
        return "/api/bills";
    }
}