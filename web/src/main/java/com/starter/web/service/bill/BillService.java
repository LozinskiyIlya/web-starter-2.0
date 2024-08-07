package com.starter.web.service.bill;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.starter.common.exception.Exceptions;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.BillTag.TagType;
import com.starter.domain.entity.Group;
import com.starter.domain.entity.User;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.BillTagRepository;
import com.starter.domain.repository.GroupRepository;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.web.fragments.BillAssistantResponse;
import com.starter.web.mapper.BillMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
