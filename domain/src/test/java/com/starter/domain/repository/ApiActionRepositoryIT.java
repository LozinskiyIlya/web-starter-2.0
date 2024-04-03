package com.starter.domain.repository;

import com.starter.domain.entity.ApiAction;
import com.starter.domain.repository.testdata.ApiActionTestData;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("For 'ApiAction' entity")
class ApiActionRepositoryIT extends AbstractRepositoryTest<ApiAction> implements ApiActionTestData {

    @Autowired
    private Repository<ApiAction> apiActionRepository;

    @Override
    public Repository<ApiAction> apiActionRepository() {
        return apiActionRepository;
    }

    @Override
    ApiAction createEntity() {
        return givenApiActionExists((a) -> {
        });
    }

}