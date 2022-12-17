package com.giraone.jobs.materialize.model;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Table("job_record")
public class JobRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String ATTRIBUTE_id = "id";
    public static final String ATTRIBUTE_jobAcceptedTimestamp = "jobAcceptedTimestamp";
    public static final String ATTRIBUTE_lastEventTimestamp = "lastEventTimestamp";
    public static final String ATTRIBUTE_lastRecordUpdateTimestamp = "lastRecordUpdateTimestamp";
    public static final String ATTRIBUTE_status = "status";
    public static final String ATTRIBUTE_pausedBucketKey = "pausedBucketKey";

    public static final String STATE_accepted = "ACCEPTED";
    public static final String STATE_scheduled = "SCHEDULED";

    @Id
    @Column("id")
    private long id;

    @Column("job_accepted_timestamp")
    private Instant jobAcceptedTimestamp;

    @Column("last_event_timestamp")
    private Instant lastEventTimestamp;

    @Column("last_record_update_timestamp")
    private Instant lastRecordUpdateTimestamp;

    @Column("status")
    private String status;

    @Column("process_id")
    private long processId;

    public JobRecord() {
    }

    public JobRecord(long id, Instant jobAcceptedTimestamp, Instant now, long processId) {
        this(id, jobAcceptedTimestamp, now, now, STATE_accepted, processId);
    }

    public JobRecord(long id, Instant jobAcceptedTimestamp,
                     Instant lastEventTimestamp, Instant lastRecordUpdateTimestamp, String status, long processId) {
        this.id = id;
        this.jobAcceptedTimestamp = jobAcceptedTimestamp;
        this.lastEventTimestamp = lastEventTimestamp;
        this.lastRecordUpdateTimestamp = lastRecordUpdateTimestamp;
        this.status = status;
        this.processId = processId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Instant getJobAcceptedTimestamp() {
        return jobAcceptedTimestamp;
    }

    public void setJobAcceptedTimestamp(Instant jobAcceptedTimestamp) {
        this.jobAcceptedTimestamp = jobAcceptedTimestamp;
    }

    public Instant getLastRecordUpdateTimestamp() {
        return lastRecordUpdateTimestamp;
    }

    public void setLastRecordUpdateTimestamp(Instant lastRecordUpdateTimestamp) {
        this.lastRecordUpdateTimestamp = lastRecordUpdateTimestamp;
    }

    public Instant getLastEventTimestamp() {
        return lastEventTimestamp;
    }

    public void setLastEventTimestamp(Instant lastEventTimestamp) {
        this.lastEventTimestamp = lastEventTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getProcessId() {
        return processId;
    }

    public void setProcessId(long processId) {
        this.processId = processId;
    }

    @Override
    public String toString() {
        return "JobRecord{" +
            "id=" + id +
            ", jobAcceptedTimestamp=" + jobAcceptedTimestamp +
            ", lastEventTimestamp=" + lastEventTimestamp +
            ", lastRecordUpdateTimestamp=" + lastRecordUpdateTimestamp +
            ", status='" + status + '\'' +
            ", processId='" + processId + '\'' +
            '}';
    }
}

