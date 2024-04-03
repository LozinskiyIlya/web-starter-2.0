package com.starter.web.job.common;

import com.starter.domain.entity.JobInvocationDetails;
import com.starter.domain.entity.JobInvocationDetails.JobInvocationStatus;
import com.starter.domain.repository.JobInvocationDetailsRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.starter.domain.entity.JobInvocationDetails.JobInvocationStatus.*;


@Service
@Slf4j
public class JobRunner {

    @Value("${spring.profiles.active:Unknown}")
    private String activeProfile;

    private final Collection<JobExecutor> jobs;
    private final JobInvocationDetailsRepository repo;

    public JobRunner(Collection<Job> jobs, JobInvocationDetailsRepository repo) {
        this.repo = repo;
        this.jobs = jobs.stream().map(JobExecutor::new).collect(Collectors.toSet());
    }

    @Scheduled(fixedRate = 300_000)//once per 5 minutes
    public void runJobsIfNecessary() {
        if (jobs == null || jobs.isEmpty() || activeProfile.equals("local") || activeProfile.equals("Unknown")) {
            return;
        }
        jobs.forEach(this::runIfNecessary);
    }

    @PreDestroy
    void stopPools() {
        jobs.forEach(JobExecutor::shutdown);
        for (var exec : jobs) {
            try {
                exec.awaitTermination();
            } catch (InterruptedException ex) {
                log.error("Failed to stop job executor {}", exec.name(), ex);
                Thread.currentThread().interrupt();
            }
        }
    }

    private void runIfNecessary(JobExecutor job) {
        log.info("Processing {}", job.name());
        final var previousRun = repo.findFirstByJobNameAndStatusInOrderByExecutedAtDesc(job.name(), SUCCESS, FAILURE);
        if (!job.shouldRun(previousRun)) {
            log.info("Skipping {}: run conditions are not satisfied", job.name());
            return;
        }
        final var details = persistNewRunDetails(job);
        updateInvocationStatus(details, IN_PROGRESS);
        try {
            log.info("Running {} #{}", job.name(), details.getId());
            job.run();
        } catch (Exception ex) {
            log.error("Failed to run job {} #{}", job.name(), details.getId(), ex);
            updateInvocationStatus(details, FAILURE);
            return;
        }
        log.info("Job's run finished {} #{}", job.name(), details.getId());
        updateInvocationStatus(details, SUCCESS);
    }

    private JobInvocationDetails persistNewRunDetails(JobExecutor job) {
        var details = new JobInvocationDetails();
        details.setJobName(job.name());
        return repo.save(details);
    }


    private void updateInvocationStatus(JobInvocationDetails details, JobInvocationStatus status) {
        details.setStatus(status);
        repo.save(details);
    }

    @RequiredArgsConstructor
    private static class JobExecutor {
        private final ExecutorService executor = Executors.newFixedThreadPool(2);

        private final Job job;

        void run() {
            executor.submit(job::run);
        }

        private String name() {
            return job.name();
        }

        private boolean shouldRun(Optional<JobInvocationDetails> details) {
            return job.shouldRun(details);
        }

        void shutdown() {
            executor.shutdown();
        }

        void awaitTermination() throws InterruptedException {
            executor.awaitTermination(15, TimeUnit.SECONDS);
        }
    }

}
