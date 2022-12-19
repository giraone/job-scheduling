package com.giraone.jobs.events;

import java.time.Instant;

public class JobStatusChangedEvent extends AbstractJobEvent {

    private String status;
    private String agentKey;
    private String pausedBucketKey;

    protected JobStatusChangedEvent() {
    }

    public JobStatusChangedEvent(String id, String processKey, Instant jobAcceptedTimestamp, Instant eventTimestamp, String status) {
        super(id, processKey, jobAcceptedTimestamp, eventTimestamp);
        this.status = status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String getStatus() {
        return this.status;
    }

    public String getAgentKey() {
        return agentKey;
    }

    public void setAgentKey(String agentKey) {
        this.agentKey = agentKey;
    }

    public void setPausedBucketKey(String pausedBucketKey) {
        this.pausedBucketKey = pausedBucketKey;
    }

    public String getPausedBucketKey() {
        return pausedBucketKey;
    }
}
