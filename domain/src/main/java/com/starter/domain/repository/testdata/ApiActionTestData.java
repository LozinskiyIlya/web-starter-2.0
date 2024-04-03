package com.starter.domain.repository.testdata;


import com.starter.domain.entity.ApiAction;
import com.starter.domain.repository.Repository;

import java.util.UUID;
import java.util.function.Consumer;

public interface ApiActionTestData {

    Repository<ApiAction> apiActionRepository();

    default ApiAction givenApiActionExists(Consumer<ApiAction> configure) {
        var action = new ApiAction();
        action.setPath("/api/v1/test");
        var metadata = new ApiAction.Metadata();
        metadata.setIp(UUID.randomUUID().toString());
        metadata.setUserAgent("User Agent");
        metadata.setParams("param1=value1");
        metadata.setHttpMethod("GET");
        metadata.setResponseCode(200);
        action.setMetadata(metadata);
        configure.accept(action);
        return apiActionRepository().saveAndFlush(action);
    }
}
