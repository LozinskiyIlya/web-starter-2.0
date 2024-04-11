package com.starter.web.populator;


import com.starter.domain.entity.BillTag;
import com.starter.domain.repository.BillTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.starter.domain.entity.BillTag.TagType;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillTagPopulator implements Populator {

    private final BillTagRepository billTagRepository;

    private final String[] billTags = {"Food", "Transport", "Entertainment", "Health", "Education", "Shopping", "Work", "Rent"};

    @Override
    public void populate() {
        for (String billTag : billTags) {
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
