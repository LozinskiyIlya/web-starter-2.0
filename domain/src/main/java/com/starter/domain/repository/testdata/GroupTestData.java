package com.starter.domain.repository.testdata;


import com.starter.domain.entity.Group;
import com.starter.domain.repository.Repository;

import java.util.function.Consumer;

public interface GroupTestData {

    Repository<Group> groupRepository();

    default Group givenGroupExists(Consumer<Group> configure) {
        var group = new Group();
        group.setTitle("group-title");
        group.setChatId(-1234567890L);
        configure.accept(group);
        return groupRepository().saveAndFlush(group);
    }
}
