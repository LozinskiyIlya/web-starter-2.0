package com.starter.domain.repository;

import com.starter.domain.entity.JobInvocationDetails;
import com.starter.domain.repository.testdata.JobInvocationDetailsTestData;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("For 'JobInvocationDetails' entity")
class JobInvocationDetailsRepositoryIT extends AbstractRepositoryTest<JobInvocationDetails> implements JobInvocationDetailsTestData {

    @Autowired
    private JobInvocationDetailsRepository repository;

    @Override
    JobInvocationDetails createEntity() {
        return givenJobInvocationDetailsExists(job -> {
        });
    }

    @Override
    public Repository<JobInvocationDetails> jobInvocationDetailsRepository() {
        return repository;
    }
}