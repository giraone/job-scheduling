package com.giraone.jobs.service.dto;

import com.giraone.jobs.domain.enumeration.JobStatusEnum;
import com.github.f4b6a3.tsid.Tsid;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.giraone.jobs.domain.JobRecord} entity.
 */
@Schema(description = "A single materialized job record.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class JobRecordDTO implements Serializable {

    private String id;

    /**
     * Timestamp, when job was accepted.
     */
    @NotNull
    @Schema(description = "Timestamp, when job was accepted.", required = true)
    private Instant jobAcceptedTimestamp;

    /**
     * Timestamp of last status change.
     */
    @NotNull
    @Schema(description = "Timestamp of last status change.", required = true)
    private Instant lastEventTimestamp;

    /**
     * Timestamp of last status change in materialized record.
     */
    @NotNull
    @Schema(description = "Timestamp of last status change in materialized record.", required = true)
    private Instant lastRecordUpdateTimestamp;

    /**
     * Job status.
     */
    @NotNull
    @Schema(description = "Job status.", required = true)
    private JobStatusEnum status;

    /**
     * Paused bucket key
     */
    @Schema(description = "Paused bucket key")
    private String pausedBucketKey;

    private ProcessDTO process;

    // ADAPTED
    public String getId() {
        return id;
    }
    // ADAPTED
    public long getLongId() {
        return Tsid.from(id).toLong();
    }
    // ADAPTED
    public void setId(String id) {
        this.id = id;
    }

    public Instant getJobAcceptedTimestamp() {
        return jobAcceptedTimestamp;
    }

    public void setJobAcceptedTimestamp(Instant jobAcceptedTimestamp) {
        this.jobAcceptedTimestamp = jobAcceptedTimestamp;
    }

    public Instant getLastEventTimestamp() {
        return lastEventTimestamp;
    }

    public void setLastEventTimestamp(Instant lastEventTimestamp) {
        this.lastEventTimestamp = lastEventTimestamp;
    }

    public Instant getLastRecordUpdateTimestamp() {
        return lastRecordUpdateTimestamp;
    }

    public void setLastRecordUpdateTimestamp(Instant lastRecordUpdateTimestamp) {
        this.lastRecordUpdateTimestamp = lastRecordUpdateTimestamp;
    }

    public JobStatusEnum getStatus() {
        return status;
    }

    public void setStatus(JobStatusEnum status) {
        this.status = status;
    }

    public String getPausedBucketKey() {
        return pausedBucketKey;
    }

    public void setPausedBucketKey(String pausedBucketKey) {
        this.pausedBucketKey = pausedBucketKey;
    }

    public ProcessDTO getProcess() {
        return process;
    }

    public void setProcess(ProcessDTO process) {
        this.process = process;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JobRecordDTO)) {
            return false;
        }

        JobRecordDTO jobRecordDTO = (JobRecordDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, jobRecordDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "JobRecordDTO{" +
            "id=" + getId() +
            ", jobAcceptedTimestamp='" + getJobAcceptedTimestamp() + "'" +
            ", lastEventTimestamp='" + getLastEventTimestamp() + "'" +
            ", lastRecordUpdateTimestamp='" + getLastRecordUpdateTimestamp() + "'" +
            ", status='" + getStatus() + "'" +
            ", pausedBucketKey='" + getPausedBucketKey() + "'" +
            ", process=" + getProcess() +
            "}";
    }
}
