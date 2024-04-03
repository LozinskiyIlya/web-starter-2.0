package com.starter.web.job;

import com.starter.domain.entity.JobInvocationDetails;
import com.starter.web.job.common.Job;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TestJob implements Job {

    public static final AtomicInteger executionCount = new AtomicInteger(0);
    public static final AtomicLong minutesBetweenRuns = new AtomicLong(0);
    public static final AtomicBoolean shouldFail = new AtomicBoolean(false);

    @Override
    public void run() {
        if(shouldFail.get()) {
            throw new RuntimeException("TestJob failed");
        }
        // Increment execution count to verify the job runs
        executionCount.incrementAndGet();
    }

    @Override
    public boolean shouldRun(Optional<JobInvocationDetails> details) {
        if (details.isEmpty()) {
            return true;
        }
        final var lastRun = details.get();
        final var minutesSinceLastRun = Duration.between(lastRun.getExecutedAt(), Instant.now()).toMinutes();
        return minutesSinceLastRun > minutesBetweenRuns.get();
    }
}
