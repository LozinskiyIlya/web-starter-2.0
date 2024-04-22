package com.starter.domain.repository;


import com.starter.domain.entity.Group;
import com.starter.domain.entity.User;

import java.util.List;

/**
 * @author ilya
 * @date 08.11.2021
 */

public interface GroupRepository extends Repository<Group> {

   List<Group> findAllByOwner(User owner);
}
