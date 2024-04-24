package com.starter.web.service.bill;


import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Bill.BillStatus;
import com.starter.domain.entity.BillTag;
import com.starter.domain.entity.BillTag.TagType;
import com.starter.domain.entity.Group;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.BillTagRepository;
import com.starter.web.fragments.BillAssistantResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final BillTagRepository billTagRepository;

    public Bill addBill(Group group, BillAssistantResponse assistantResponse) {
        //todo: move to Mapstruct
        final var bill = new Bill();
        bill.setGroup(group);
        bill.setBuyer(assistantResponse.getBuyer());
        bill.setSeller(assistantResponse.getSeller());
        bill.setPurpose(assistantResponse.getPurpose());
        bill.setCurrency(assistantResponse.getCurrency());
        bill.setAmount(assistantResponse.getAmount());
        bill.setMentionedDate(assistantResponse.getMentionedDate());
        final var userTags = billTagRepository.findAllByUser(group.getOwner());
        final var defaultTags = billTagRepository.findAllByTagType(TagType.DEFAULT);
        final var tags = Stream.concat(userTags.stream(), defaultTags.stream())
                .filter(tag -> Stream.of(assistantResponse.getTags()).anyMatch(tag.getName()::equalsIgnoreCase))
                .collect(Collectors.toSet());
        bill.setTags(tags);
        bill.setStatus(BillStatus.NEW);
        return billRepository.save(bill);
    }
}
