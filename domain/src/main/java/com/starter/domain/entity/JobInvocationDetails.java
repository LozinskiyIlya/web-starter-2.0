package com.starter.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;

import static java.time.Instant.now;

@Getter
@Setter
@Entity
@Table(name = "job_invocation_details")
@SQLDelete(sql = "UPDATE job_invocation_details SET state='DELETED' WHERE id=?")
@Loader(namedQuery = "findNonDeletedJobInvocationDetailsById")
@NamedQuery(name = "findNonDeletedJobInvocationDetailsById", query = "SELECT d FROM JobInvocationDetails d WHERE d.id = ?1 AND d.state <> 'DELETED'")
@Where(clause = "state != 'DELETED'")
public class JobInvocationDetails extends AbstractEntity {

    @NotNull
    @Column(name = "job_name")
    private String jobName;

    @NotNull
    @Column(name = "executed_at")
    private Instant executedAt = now();

    @NotNull
    @Column(name = "status")
    private JobInvocationStatus status = JobInvocationStatus.NOT_RUN;

    public enum JobInvocationStatus {
        NOT_RUN, IN_PROGRESS, SUCCESS, FAILURE
    }

}
