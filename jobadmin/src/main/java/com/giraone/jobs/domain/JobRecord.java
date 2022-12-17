package com.giraone.jobs.domain;

import com.giraone.jobs.domain.enumeration.JobStatusEnum;
import java.io.Serializable;
import java.time.Instant;
import javax.persistence.*;
import javax.validation.constraints.*;

/**
 * A single materialized job record.
 */
@Entity
@Table(name = "job_record")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class JobRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    /**
     * Timestamp, when job was accepted.
     */
    @NotNull
    @Column(name = "job_accepted_timestamp", nullable = false)
    private Instant jobAcceptedTimestamp;

    /**
     * Timestamp of last status change.
     */
    @NotNull
    @Column(name = "last_event_timestamp", nullable = false)
    private Instant lastEventTimestamp;

    /**
     * Timestamp of last status change in materialized record.
     */
    @NotNull
    @Column(name = "last_record_update_timestamp", nullable = false)
    private Instant lastRecordUpdateTimestamp;

    /**
     * Job status.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatusEnum status;

    /**
     * Paused bucket key
     */
    @Column(name = "paused_bucket_key")
    private String pausedBucketKey;

    /**
     * Process to which job belongs.
     */
    @ManyToOne(optional = false)
    @NotNull
    private Process process;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public JobRecord id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getJobAcceptedTimestamp() {
        return this.jobAcceptedTimestamp;
    }

    public JobRecord jobAcceptedTimestamp(Instant jobAcceptedTimestamp) {
        this.setJobAcceptedTimestamp(jobAcceptedTimestamp);
        return this;
    }

    public void setJobAcceptedTimestamp(Instant jobAcceptedTimestamp) {
        this.jobAcceptedTimestamp = jobAcceptedTimestamp;
    }

    public Instant getLastEventTimestamp() {
        return this.lastEventTimestamp;
    }

    public JobRecord lastEventTimestamp(Instant lastEventTimestamp) {
        this.setLastEventTimestamp(lastEventTimestamp);
        return this;
    }

    public void setLastEventTimestamp(Instant lastEventTimestamp) {
        this.lastEventTimestamp = lastEventTimestamp;
    }

    public Instant getLastRecordUpdateTimestamp() {
        return this.lastRecordUpdateTimestamp;
    }

    public JobRecord lastRecordUpdateTimestamp(Instant lastRecordUpdateTimestamp) {
        this.setLastRecordUpdateTimestamp(lastRecordUpdateTimestamp);
        return this;
    }

    public void setLastRecordUpdateTimestamp(Instant lastRecordUpdateTimestamp) {
        this.lastRecordUpdateTimestamp = lastRecordUpdateTimestamp;
    }

    public JobStatusEnum getStatus() {
        return this.status;
    }

    public JobRecord status(JobStatusEnum status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(JobStatusEnum status) {
        this.status = status;
    }

    public String getPausedBucketKey() {
        return this.pausedBucketKey;
    }

    public JobRecord pausedBucketKey(String pausedBucketKey) {
        this.setPausedBucketKey(pausedBucketKey);
        return this;
    }

    public void setPausedBucketKey(String pausedBucketKey) {
        this.pausedBucketKey = pausedBucketKey;
    }

    public Process getProcess() {
        return this.process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public JobRecord process(Process process) {
        this.setProcess(process);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JobRecord)) {
            return false;
        }
        return id != null && id.equals(((JobRecord) o).id);
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "JobRecord{" +
            "id=" + getId() +
            ", jobAcceptedTimestamp='" + getJobAcceptedTimestamp() + "'" +
            ", lastEventTimestamp='" + getLastEventTimestamp() + "'" +
            ", lastRecordUpdateTimestamp='" + getLastRecordUpdateTimestamp() + "'" +
            ", status='" + getStatus() + "'" +
            ", pausedBucketKey='" + getPausedBucketKey() + "'" +
            "}";
    }
}
