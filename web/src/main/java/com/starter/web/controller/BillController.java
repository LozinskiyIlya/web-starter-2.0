package com.starter.web.controller;


import com.starter.common.events.BillConfirmedEvent;
import com.starter.common.exception.Exceptions;
import com.starter.common.service.CurrentUserService;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.BillTag;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.BillTagRepository;
import com.starter.domain.repository.GroupRepository;
import com.starter.web.dto.BillDto;
import com.starter.web.fragments.BillAssistantResponse;
import com.starter.web.fragments.RecognitionRequest;
import com.starter.web.mapper.BillMapper;
import com.starter.web.service.AwsS3Service.AttachmentType;
import com.starter.web.service.MessageProcessor;
import com.starter.web.service.bill.BillService;
import com.starter.web.service.openai.OpenAiAssistant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.starter.telegram.service.TelegramBillService.NOT_RECOGNIZED_MESSAGE;
import static com.starter.telegram.service.TelegramBillService.PROCESSING_ERROR_MESSAGE;
import static com.starter.web.filter.PremiumFilter.Premium;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/bills")
@Schema(title = "Bill-related requests")
public class BillController {

    private final CurrentUserService currentUserService;
    private final GroupRepository groupRepository;
    private final BillRepository billRepository;
    private final BillTagRepository billTagRepository;
    private final BillMapper billMapper;
    private final ApplicationEventPublisher publisher;
    private final MessageProcessor messageProcessor;
    private final OpenAiAssistant openAiAssistant;
    private final BillService billService;

    @GetMapping("/{billId}/preview")
    public BillDto getBillPreview(@PathVariable UUID billId) {
        return billRepository.findById(billId)
                .map(billMapper::toDto)
                .orElseThrow(Exceptions.ResourceNotFoundException::new);
    }

    @GetMapping("/{billId}")
    public BillDto getBill(@PathVariable UUID billId) {
        return billRepository.findById(billId)
                .map(billMapper::toDto)
                .orElseThrow(Exceptions.ResourceNotFoundException::new);
    }

    @GetMapping("/{billId}/attachment")
    public URI getBillAttachment(@PathVariable UUID billId) {
        return billRepository.findAttachmentById(billId);
    }

    @PostMapping("")
    @Operation(summary = "Add bill", description = "Add bill to the group specified in dto")
    public UUID updateBill(@RequestBody @Valid BillDto billDto) {
        final var currentUser = currentUserService.getUser().orElseThrow();
        final var group = groupRepository.findById(billDto.getGroup().getId())
                .orElseThrow(Exceptions.ResourceNotFoundException::new);
        if (!group.getOwner().getId().equals(currentUser.getId())) {
            throw new Exceptions.WrongUserException("You can't add bills to this group");
        }

        var bill = billMapper.updateEntityFromDto(billDto, new Bill());
        bill.setGroup(group);
        bill.setStatus(Bill.BillStatus.NEW);
        bill.setMessageId(Bill.DEFAULT_MESSAGE_ID);
        bill = billRepository.saveAndFlush(bill);
        publisher.publishEvent(new BillConfirmedEvent(this, bill.getId()));
        return bill.getId();
    }

    @PostMapping("/parse")
    @Operation(summary = "Parse details", description = "Add bill by parsing a text or an image")
    public UUID parseBill(@RequestBody @Valid RecognitionRequest request) {
        final var currentUser = currentUserService.getUser().orElseThrow();
        final var group = billService.selectGroupForAddingBill(request.getGroupId(), currentUser);
        BillAssistantResponse response;
        String base64 = null;
        try {
            if (request.getType() == RecognitionRequest.RecognitionType.TEXT) {
                response = openAiAssistant.runTextPipeline(currentUser.getId(), request.getDetails(), group.getDefaultCurrency());
            } else if (request.getType() == RecognitionRequest.RecognitionType.IMAGE) {
                base64 = request.getDetails();
                response = openAiAssistant.runFilePipeline(currentUser.getId(), base64, "", group.getDefaultCurrency());
            } else {
                throw new Exceptions.RecognitionException("Invalid recognition type");
            }
        } catch (Exception exception) {
            throw new Exceptions.RecognitionException(exception, PROCESSING_ERROR_MESSAGE);
        }
        if (messageProcessor.shouldSave(group, response)) {
            final var created = billService.addBill(group, response);
            created.setStatus(Bill.BillStatus.SENT);
            billService.addAttachment(created, base64, request.getFileName(), AttachmentType.BASE_64);
            return billRepository.save(created).getId();
        }
        throw new Exceptions.RecognitionException(NOT_RECOGNIZED_MESSAGE);
    }

    @PostMapping("/{billId}")
    @Operation(summary = "Update bill", description = "Update bill by id, only owner can do this")
    public void updateBill(@PathVariable UUID billId, @RequestBody @Valid BillDto billDto) {
        final var bill = billRepository.findById(billId)
                .orElseThrow(Exceptions.ResourceNotFoundException::new);
        final var currentUser = currentUserService.getUser().orElseThrow();
        if (!bill.getGroup().getOwner().getId().equals(currentUser.getId())) {
            throw new Exceptions.WrongUserException("You can't update this bill");
        }
        final var updated = billMapper.updateEntityFromDto(billDto, bill);
        billRepository.save(updated);
        publisher.publishEvent(new BillConfirmedEvent(this, updated.getId()));
    }

    @DeleteMapping("/{billId}")
    @Operation(summary = "Skip bill", description = "Mark bill as skipped, NOT deleting it entirely")
    public void skipBill(@PathVariable UUID billId) {
        final var bill = billRepository.findById(billId)
                .orElseThrow(Exceptions.ResourceNotFoundException::new);
        final var currentUser = currentUserService.getUser().orElseThrow();
        if (!bill.getGroup().getOwner().getId().equals(currentUser.getId())) {
            throw new Exceptions.WrongUserException("You can't skip this bill");
        }
        billService.skipBill(bill, currentUser);
    }

    @DeleteMapping("")
    @Operation(summary = "Skip bills in batch", description = "Mark bills as skipped, NOT deleting them entirely")
    public void skipAll(@RequestBody List<UUID> ids) {
        // on empty list do nothing
        if (ids.isEmpty()) {
            return;
        }
        // check rights to skip
        final var bills = billRepository.findAllById(ids);
        final var currentUser = currentUserService.getUser().orElseThrow();
        if (!bills.get(0).getGroup().getOwner().getId().equals(currentUser.getId())) {
            throw new Exceptions.WrongUserException("You can't skip this bill");
        }
        // skip all bills
        bills.forEach(bill -> billService.skipBill(bill, currentUser));
    }

    @GetMapping("/tags")
    public List<BillDto.BillTagDto> getTags() {
        final var userTags = currentUserService.getUser()
                .map(billTagRepository::findAllByUser)
                .orElse(Set.of());
        final var defaultTags = billTagRepository.findAllByTagType(BillTag.TagType.DEFAULT);
        return Stream.concat(userTags.stream(), defaultTags.stream())
                .map(billMapper::toTagDto)
                .toList();
    }

    @Premium
    @PostMapping("/tags")
    @Operation(summary = "Create tag", description = "Create custom tag from DTO, available only for premium users")
    public UUID createTag(@RequestBody @Valid BillDto.BillTagDto dto) {
        final var current = currentUserService.getUser().orElseThrow();
        final var newTag = billMapper.toTagEntity(dto);
        newTag.setUser(current);
        try {
            return billTagRepository.saveAndFlush(newTag).getId();
        } catch (DataIntegrityViolationException e) {
            throw new Exceptions.ValidationException("Tag with this name already exists");
        }
    }

    @DeleteMapping("/tags/{tagId}")
    @Operation(summary = "Delete tag", description = "Delete tag by id, only owner can do this")
    public void deleteTag(@PathVariable UUID tagId) {
        final var currentUser = currentUserService.getUser().orElseThrow();
        billService.deleteTag(tagId, currentUser);
    }
}
