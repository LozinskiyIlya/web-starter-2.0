package com.starter.domain.repository.testdata;


import com.starter.domain.entity.JobInvocationDetails;
import com.starter.domain.repository.Repository;

import java.time.Instant;
import java.util.function.Consumer;

public interface JobInvocationDetailsTestData {

    Repository<JobInvocationDetails> jobInvocationDetailsRepository();

    default JobInvocationDetails givenJobInvocationDetailsExists(Consumer<JobInvocationDetails> configure) {
        var jobInvocationDetails = new JobInvocationDetails();
        jobInvocationDetails.setJobName("job_name");
        jobInvocationDetails.setStatus(JobInvocationDetails.JobInvocationStatus.IN_PROGRESS);
        jobInvocationDetails.setCreatedAt(Instant.EPOCH);
        configure.accept(jobInvocationDetails);
        return jobInvocationDetailsRepository().saveAndFlush(jobInvocationDetails);
    }
}
