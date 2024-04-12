package com.starter.web.populator;


import com.starter.domain.entity.BillTag;
import com.starter.domain.repository.BillTagRepository;
import com.starter.web.configuration.openai.AssistantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.starter.domain.entity.BillTag.TagType;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillTagPopulator implements Populator {

    private final BillTagRepository billTagRepository;
    private final AssistantProperties assistantProperties;

    @Override
    public void populate() {
        for (String billTag : assistantProperties.getBillTags()) {
            billTagRepository.findByNameAndTagType(billTag, TagType.DEFAULT).ifPresentOrElse(r -> {
            }, () -> {
                BillTag newBillTag = new BillTag();
                newBillTag.setName(billTag);
                billTagRepository.save(newBillTag);
            });
        }
        billTagRepository.flush();
    }
}
