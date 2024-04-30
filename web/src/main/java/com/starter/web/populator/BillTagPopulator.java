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
        for (int i = 0; i < assistantProperties.getBillTags().length; i++) {
            final var billTag = assistantProperties.getBillTags()[i];
            final var color = assistantProperties.getTagsColors()[i];
            billTagRepository.findByNameAndTagType(billTag, TagType.DEFAULT).ifPresentOrElse(r -> {
            }, () -> {
                BillTag newBillTag = new BillTag();
                newBillTag.setName(billTag);
                newBillTag.setHexColor(color);
                billTagRepository.save(newBillTag);
            });
        }
        billTagRepository.flush();
    }
}
