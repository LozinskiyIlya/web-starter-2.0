package com.starter.domain.repository;


import com.starter.domain.entity.BillTag;
import com.starter.domain.entity.BillTag.TagType;
import com.starter.domain.entity.User;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * @author ilya
 * @date 08.11.2021
 */

public interface BillTagRepository extends Repository<BillTag> {

    Set<BillTag> findAllByUser(User user);

    Set<BillTag> findAllByUserAndNameIgnoreCaseIn(User user, Collection<String> name);

    Optional<BillTag> findByNameAndTagType(String name, TagType tagType);

    Set<BillTag> findAllByTagType(TagType tagType);
}
