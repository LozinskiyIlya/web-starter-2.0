package com.starter.domain.repository;


import com.starter.domain.entity.BillTag;
import com.starter.domain.entity.BillTag.TagType;
import com.starter.domain.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * @author ilya
 * @date 08.11.2021
 */

public interface BillTagRepository extends Repository<BillTag> {

    List<BillTag> findAllByUser(User user);

    List<BillTag> findAllByTagType(TagType tagType);

    Optional<BillTag> findByNameAndTagType(String name, TagType tagType);
}
