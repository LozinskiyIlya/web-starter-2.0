package com.starter.web.service.bill;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.BillTag.TagType;
import com.starter.domain.entity.Group;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.BillTagRepository;
import com.starter.web.fragments.BillAssistantResponse;
import com.starter.web.mapper.BillMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {

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
}
