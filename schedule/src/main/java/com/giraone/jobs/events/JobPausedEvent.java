package com.giraone.jobs.events;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serial;
import java.time.Instant;

public class JobPausedEvent extends AbstractJobStatusChangedEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    private String pausedBucketKey;

    public JobPausedEvent() {
    }

    public JobPausedEvent(String pausedBucketKey, JobAcceptedEvent jobAcceptedEvent) {
        this(jobAcceptedEvent.getId(), jobAcceptedEvent.getProcessKey(), jobAcceptedEvent.getJobAcceptedTimestamp(),
            Instant.now(), jobAcceptedEvent.getPayload(), pausedBucketKey);
    }

    public JobPausedEvent(String id, String processKey, Instant jobAcceptedTimestamp,
                          Instant eventTimestamp, String payload, String pausedBucketKey) {
        super(id, processKey, jobAcceptedTimestamp, eventTimestamp, payload, "PAUSED");
        this.pausedBucketKey = pausedBucketKey;
    }

    @Override
    public String getStatus() {
        return "PAUSED";
    }

    @JsonIgnore
    public String getBucketSuffix() {
        return pausedBucketKey;
    }
}