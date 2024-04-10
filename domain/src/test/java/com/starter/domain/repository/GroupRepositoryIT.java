package com.starter.domain.repository;

import com.starter.domain.entity.Group;
import com.starter.domain.repository.testdata.GroupTestDataCreator;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("For 'Group' entity")
class GroupRepositoryIT extends AbstractRepositoryTest<Group> {

    @Autowired
    private GroupTestDataCreator groupCreator;

    @Override
    Group createEntity() {
        return groupCreator.givenGroupExists(g -> {
        });
    }
}