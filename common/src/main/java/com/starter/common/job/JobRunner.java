package com.starter.common.job;

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
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
@Slf4j
public class JobRunner {

    @Value("${spring.profiles.active:Unknown}")
    private String activeProfile;

    private static final Set<String> IGNORED_PROFILES = Set.of(
//            "local",
            "Unknown"
    );

    private final Collection<JobExecutor> jobs;
    private final JobInvocationDetailsRepository repo;

    public JobRunner(Collection<Job> jobs, JobInvocationDetailsRepository repo) {
        this.repo = repo;
        this.jobs = jobs.stream().map(JobExecutor::new).collect(Collectors.toSet());
    }

    @Scheduled(cron = "0 */5 * * * *") // once per 5 minutes at the start of the minute
    public void runJobsIfNecessary() {
        if (jobs == null || jobs.isEmpty() || IGNORED_PROFILES.contains(activeProfile)) {
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
        final var inProgress = repo.findFirstByJobNameAndStatusInOrderByCreatedAtDesc(job.name(), JobInvocationStatus.IN_PROGRESS);
        if (inProgress.isPresent()) {
            log.info("Skipping {}: job is already running", job.name());
            return;
        }
        final var previousRun = repo.findFirstByJobNameAndStatusInOrderByCreatedAtDesc(job.name(), JobInvocationStatus.SUCCESS, JobInvocationStatus.FAILURE);
        if (!job.shouldRun(previousRun)) {
            log.info("Skipping {}: run conditions are not satisfied", job.name());
            return;
        }
        final var details = persistNewRunDetails(job);
        updateInvocationStatus(details, JobInvocationStatus.IN_PROGRESS);
        try {
            log.info("Running {} #{}", job.name(), details.getId());
            final var run = job.run();
            run.get();
        } catch (Exception ex) {
            log.error("Failed to run job {} #{}", job.name(), details.getId(), ex);
            updateInvocationStatus(details, JobInvocationStatus.FAILURE);
            return;
        }
        log.info("Job's run finished {} #{}", job.name(), details.getId());
        updateInvocationStatus(details, JobInvocationStatus.SUCCESS);
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

        Future<?> run() {
            return executor.submit(job::run);
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

        @SuppressWarnings("ResultOfMethodCallIgnored")
        void awaitTermination() throws InterruptedException {
            executor.awaitTermination(15, TimeUnit.SECONDS);
        }
    }

}
