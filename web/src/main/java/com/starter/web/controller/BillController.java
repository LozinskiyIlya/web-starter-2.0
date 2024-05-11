package com.starter.web.controller;


import com.pengrad.telegrambot.TelegramBot;
import com.starter.common.events.BillConfirmedEvent;
import com.starter.common.exception.Exceptions;
import com.starter.common.service.CurrentUserService;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.BillTag;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.BillTagRepository;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import com.starter.web.dto.BillDto;
import com.starter.web.mapper.BillMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.starter.telegram.service.TelegramBillService.SelfMadeMessage;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/bills")
@Schema(title = "Bill-related requests")
public class BillController {

    private final CurrentUserService currentUserService;
    private final BillRepository billRepository;
    private final BillTagRepository billTagRepository;
    private final BillMapper billMapper;
    private final TelegramMessageRenderer messageRenderer;
    private final TelegramBot telegramBot;
    private final ApplicationEventPublisher publisher;

    @GetMapping("/{billId}")
    public BillDto getBill(@PathVariable UUID billId) {
        return billRepository.findById(billId)
                .map(billMapper::toDto)
                .orElseThrow(Exceptions.ResourceNotFoundException::new);
    }

    @PostMapping("/{billId}")
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
        bill.setStatus(Bill.BillStatus.SKIPPED);
        billRepository.save(bill);
        //todo: refactor this
        if (bill.getMessageId() == null) {
            return;
        }
        final var tgMessage = new SelfMadeMessage();
        tgMessage.setMessageId(bill.getMessageId());
        final var message = messageRenderer.renderBillSkipped(currentUser.getUserInfo().getTelegramChatId(), bill, tgMessage);
        telegramBot.execute(message);
        //todo: refactor this
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
}
