package com.starter.domain.repository.testdata;


import com.starter.domain.entity.UserInfo;
import com.starter.domain.repository.Repository;

import java.util.function.Consumer;

public interface UserInfoTestData {

    Repository<UserInfo> userInfoRepository();

    default UserInfo givenUserInfoExists(Consumer<UserInfo> configure) {
        var userInfo = new UserInfo();
        userInfo.setLastName("last name");
        userInfo.setFirstName("first name");
        configure.accept(userInfo);
        return userInfoRepository().saveAndFlush(userInfo);
    }

}
