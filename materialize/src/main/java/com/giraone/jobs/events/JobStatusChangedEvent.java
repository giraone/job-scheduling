package com.giraone.jobs.events;

import java.time.Instant;

public class JobStatusChangedEvent extends AbstractJobEvent {

    private String status;
    private String pausedBucketKey;

    protected JobStatusChangedEvent() {
    }

    public JobStatusChangedEvent(String id, String processKey, Instant eventTimestamp, String status) {
        super(id, processKey, eventTimestamp);
        this.status = status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String getStatus() {
        return this.status;
    }

    public void setPausedBucketKey(String pausedBucketKey) {
        this.pausedBucketKey = pausedBucketKey;
    }

    public String getPausedBucketKey() {
        return pausedBucketKey;
    }
}
