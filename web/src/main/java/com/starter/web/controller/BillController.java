package com.starter.web.controller;


import com.pengrad.telegrambot.TelegramBot;
import com.starter.common.events.BillConfirmedEvent;
import com.starter.common.exception.Exceptions;
import com.starter.common.service.CurrentUserService;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.BillTag;
import com.starter.domain.entity.UserInfo;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.BillTagRepository;
import com.starter.domain.repository.GroupRepository;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import com.starter.web.dto.BillDto;
import com.starter.web.mapper.BillMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.PreDestroy;
import jakarta.validation.Valid;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.starter.telegram.service.TelegramBillService.SelfMadeTelegramMessage;
import static com.starter.telegram.service.render.TelegramStaticRenderer.renderBillSkipped;

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
    private final TelegramMessageRenderer messageRenderer;
    private final TelegramBot telegramBot;
    private final ApplicationEventPublisher publisher;
    private final ExecutorService billMessageExecutor = Executors.newFixedThreadPool(4);

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @PreDestroy
    public void destroy() {
        billMessageExecutor.shutdown();
        try {
            billMessageExecutor.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            log.error("Failed to stop executor", ex);
            Thread.currentThread().interrupt();
        }
    }

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

    @PostMapping("")
    @Operation(summary = "Add bill", description = "Add bill to the group specified in dto")
    public BillCreationResponse updateBill(@RequestBody @Valid BillDto billDto) {
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
        return BillCreationResponse.builder().id(bill.getId()).build();
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
        skipBill(bill, currentUser.getUserInfo());
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
        bills.forEach(bill -> skipBill(bill, currentUser.getUserInfo()));
    }

    private void skipBill(Bill bill, UserInfo currentUserInfo) {
        bill.setStatus(Bill.BillStatus.SKIPPED);
        billRepository.save(bill);
        billMessageExecutor.submit(() -> {
            final var tgMessage = new SelfMadeTelegramMessage();
            tgMessage.setMessageId(bill.getMessageId());
            final var message = renderBillSkipped(currentUserInfo.getTelegramChatId(), bill, tgMessage);
            telegramBot.execute(message);
        });
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

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class BillCreationResponse {
        private UUID id;
    }
}
