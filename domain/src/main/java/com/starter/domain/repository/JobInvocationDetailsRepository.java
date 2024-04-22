package com.starter.domain.repository;

import com.starter.domain.entity.JobInvocationDetails;

import java.util.Optional;


public interface JobInvocationDetailsRepository extends Repository<JobInvocationDetails> {
    Optional<JobInvocationDetails> findFirstByJobNameAndStatusInOrderByCreatedAtDesc(String jobName, JobInvocationDetails.JobInvocationStatus... statuses);

}
