package com.starter.web.service.bill;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengrad.telegrambot.TelegramBot;
import com.starter.common.exception.Exceptions;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.BillTag.TagType;
import com.starter.domain.entity.Group;
import com.starter.domain.entity.User;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.BillTagRepository;
import com.starter.domain.repository.GroupRepository;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.telegram.service.TelegramBillService;
import com.starter.web.fragments.BillAssistantResponse;
import com.starter.web.mapper.BillMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.starter.telegram.service.render.TelegramStaticRenderer.renderBillSkipped;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {

    private final GroupRepository groupRepository;
    private final UserInfoRepository userInfoRepository;
    private final BillRepository billRepository;
    private final BillTagRepository billTagRepository;
    private final BillMapper billMapper;
    private final ObjectMapper objectMapper;
    private final TelegramBot telegramBot;

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

    @SneakyThrows
    public Bill addBill(Group group, BillAssistantResponse assistantResponse) {
        final var userTags = billTagRepository.findAllByUser(group.getOwner());
        final var defaultTags = billTagRepository.findAllByTagType(TagType.DEFAULT);
        final var tags = Stream.concat(userTags.stream(), defaultTags.stream())
                .filter(tag -> Stream.of(assistantResponse.getTags()).anyMatch(tag.getName()::equalsIgnoreCase))
                .collect(Collectors.toSet());
        final var bill = billMapper.fromModelResponse(assistantResponse, group, tags);
        bill.setModelResponse(objectMapper.writeValueAsString(assistantResponse));
        return billRepository.save(bill);
    }

    public void skipBill(Bill bill, User current) {
        final var userInfo = userInfoRepository.findOneByUser(current).orElseThrow();
        bill.setStatus(Bill.BillStatus.SKIPPED);
        billRepository.save(bill);
        billMessageExecutor.submit(() -> {
            final var tgMessage = new TelegramBillService.SelfMadeTelegramMessage(bill.getMessageId());
            final var message = renderBillSkipped(userInfo.getTelegramChatId(), bill, tgMessage);
            telegramBot.execute(message);
        });
    }

    public Group selectGroupForAddingBill(UUID groupId, User current) {
        if (groupId == null) {
            // select personal group as default
            final var userInfo = userInfoRepository.findOneByUser(current).orElseThrow();
            final var chatId = userInfo.getTelegramChatId();
            return groupRepository.findByChatId(chatId).orElseThrow(Exceptions.ResourceNotFoundException::new);
        }
        final var group = groupRepository.findById(groupId).orElseThrow(Exceptions.ResourceNotFoundException::new);
        if (!group.getOwner().getId().equals(current.getId())) {
            throw new Exceptions.WrongUserException("You can't add bills to this group");
        }
        return group;
    }

}
