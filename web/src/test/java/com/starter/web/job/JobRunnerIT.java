package com.starter.web.job;

import com.starter.domain.entity.JobInvocationDetails;
import com.starter.domain.repository.JobInvocationDetailsRepository;
import com.starter.domain.repository.Repository;
import com.starter.domain.repository.testdata.JobInvocationDetailsTestData;
import com.starter.web.AbstractSpringIntegrationTest;
import com.starter.web.job.common.JobRunner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Job runs successfully")
    public void jobRuns() {
        jobRunner.runJobsIfNecessary();
        assertThat(TestJob.executionCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Successful job execution updates status")
    public void successfulJobExecutionUpdatesStatus() {
        jobRunner.runJobsIfNecessary();
        final var latestDetails = jobInvocationDetailsRepository.findFirstByJobNameAndStatusInOrderByExecutedAtDesc(TestJob.class.getSimpleName(), JobInvocationDetails.JobInvocationStatus.SUCCESS);
        Assertions.assertTrue(latestDetails.isPresent());
    }


    @Test
    @DisplayName("Job failure updates status")
    public void jobFailureUpdatesStatus() {
        TestJob.shouldFail.set(true);
        jobRunner.runJobsIfNecessary();

        final var latestDetails = jobInvocationDetailsRepository.findFirstByJobNameAndStatusInOrderByExecutedAtDesc(TestJob.class.getSimpleName(), JobInvocationDetails.JobInvocationStatus.FAILURE);
        Assertions.assertTrue(latestDetails.isPresent());
    }

    @Test
    @DisplayName("Job does not run if last run was not enough time ago")
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

        // The job was not executed
        assertThat(TestJob.executionCount.get()).isEqualTo(0);
    }

    @Test
    @DisplayName("Job runs if last run was enough time ago")
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

        // The job was executed
        assertThat(TestJob.executionCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Job does not run if another instance is already running")
    public void jobDoesNotRunIfAnotherInstanceIsRunning() {
        // Given a job run that is in progress
        givenJobInvocationDetailsExists(details -> {
            details.setJobName(TestJob.class.getSimpleName());
            details.setStatus(JobInvocationDetails.JobInvocationStatus.IN_PROGRESS);
        });

        jobRunner.runJobsIfNecessary();

        // The job was not executed
        assertThat(TestJob.executionCount.get()).isEqualTo(0);
    }


    @Override
    public Repository<JobInvocationDetails> jobInvocationDetailsRepository() {
        return jobInvocationDetailsRepository;
    }
}
