package com.starter.web.job;

import com.starter.domain.entity.JobInvocationDetails;
import com.starter.domain.repository.JobInvocationDetailsRepository;
import com.starter.domain.repository.Repository;
import com.starter.domain.repository.testdata.JobInvocationDetailsTestData;
import com.starter.web.AbstractSpringIntegrationTest;
import com.starter.web.job.common.JobRunner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;


public class JobRunnerIT extends AbstractSpringIntegrationTest implements JobInvocationDetailsTestData {

    @Autowired
    private JobRunner jobRunner;

    @Autowired
    private JobInvocationDetailsRepository jobInvocationDetailsRepository;

    @BeforeEach
    void setUp() {
        // Reset the TestJob execution history and settings
        jobInvocationDetailsRepository.deleteAll();
        TestJob.executionCount.set(0);
        TestJob.minutesBetweenRuns.set(0);
        TestJob.shouldFail.set(false);
    }

    @Test
    public void jobRuns() {
        jobRunner.runJobsIfNecessary();
        assertThat(TestJob.executionCount.get()).isEqualTo(1);
    }

    @Test
    public void successfulJobExecutionUpdatesStatus() {
        jobRunner.runJobsIfNecessary();
        final var latestDetails = jobInvocationDetailsRepository.findFirstByJobNameAndStatusInOrderByExecutedAtDesc(TestJob.class.getSimpleName(), JobInvocationDetails.JobInvocationStatus.SUCCESS);
        Assertions.assertTrue(latestDetails.isPresent());
    }


    @Test
    public void jobFailureUpdatesStatus() {
        TestJob.shouldFail.set(true);
        jobRunner.runJobsIfNecessary();

        final var latestDetails = jobInvocationDetailsRepository.findFirstByJobNameAndStatusInOrderByExecutedAtDesc(TestJob.class.getSimpleName(), JobInvocationDetails.JobInvocationStatus.FAILURE);
        Assertions.assertTrue(latestDetails.isPresent());
    }

    @Test
    public void jobDoesNotRunIfLastRunIsWithinThreshold() {
        // Set the threshold
        TestJob.minutesBetweenRuns.set(60); // 60 minutes

        // Given a job run that occurred 30 minutes ago
        givenJobInvocationDetailsExists(details -> {
            details.setJobName(TestJob.class.getSimpleName());
            details.setStatus(JobInvocationDetails.JobInvocationStatus.SUCCESS);
            details.setExecutedAt(Instant.now().minus(Duration.ofMinutes(30)));
        });

        jobRunner.runJobsIfNecessary();

        // Verify the job was not executed
        assertThat(TestJob.executionCount.get()).isEqualTo(0);
    }

    @Test
    public void jobRunsIfLastRunExceedsThreshold() {
        // Set the threshold
        TestJob.minutesBetweenRuns.set(60); // 60 minutes

        // Given a job run that occurred 90 minutes ago
        givenJobInvocationDetailsExists(details -> {
            details.setJobName(TestJob.class.getSimpleName());
            details.setStatus(JobInvocationDetails.JobInvocationStatus.SUCCESS);
            details.setExecutedAt(Instant.now().minus(Duration.ofMinutes(90)));
        });

        jobRunner.runJobsIfNecessary();

        // Verify the job was executed
        assertThat(TestJob.executionCount.get()).isEqualTo(1);
    }


    @Override
    public Repository<JobInvocationDetails> jobInvocationDetailsRepository() {
        return jobInvocationDetailsRepository;
    }
}
