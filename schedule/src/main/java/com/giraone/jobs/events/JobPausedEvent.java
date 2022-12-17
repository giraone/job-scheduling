package com.giraone.jobs.events;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serial;
import java.time.Instant;

public class JobPausedEvent extends AbstractJobStatusChangedEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    private int bucket;

    public JobPausedEvent() {
    }

    public JobPausedEvent(int bucket, JobAcceptedEvent jobAcceptedEvent) {
        this(jobAcceptedEvent.getId(), jobAcceptedEvent.getProcessKey(), Instant.now(), jobAcceptedEvent.getPayload(), bucket);
    }

    public JobPausedEvent(String id, String processKey, Instant eventTimestamp, String payload, int bucket) {
        super(id, processKey, eventTimestamp, payload, "PAUSED");
        this.bucket = bucket;
    }

    @Override
    public String getStatus() {
        return "PAUSED";
    }

    @JsonIgnore
    public String getBucketSuffix() {
        return String.format("B%02d", bucket);
    }
}