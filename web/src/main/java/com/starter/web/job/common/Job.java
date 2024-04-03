package com.starter.web.job.common;


import com.starter.domain.entity.JobInvocationDetails;

import java.util.Optional;

/**
 * scheduled task
 */
public interface Job {

    default String name() {
        return this.getClass().getSimpleName();
    }

    default boolean shouldRun(Optional<JobInvocationDetails> details) {
        return true;
    }

    void run();
}
