package com.giraone.jobs.events;

import java.time.Instant;

public class JobStatusChangedEvent extends AbstractJobEvent {

    private String status;

    protected JobStatusChangedEvent() {
    }

    public JobStatusChangedEvent(long id, String processKey, Instant eventTimestamp, String status) {
        super(id, processKey, eventTimestamp);
        this.status = status;
    }

    @Override
    public String getStatus() {
        return this.status;
    }
}
