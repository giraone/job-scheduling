package com.giraone.jobs.events;

import java.time.Instant;

public abstract class AbstractJobStatusChangedEvent extends AbstractJobEvent {

    protected String status;

    protected AbstractJobStatusChangedEvent() {
    }

    protected AbstractJobStatusChangedEvent(long id, String processKey, Instant eventTimestamp, String payload, String status) {
        super(id, processKey, eventTimestamp, payload);
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
            "id=" + id +
            ", eventTimestamp=" + eventTimestamp +
            ", processKey='" + processKey + '\'' +
            ", payload='" + payload + '\'' +
            ", status='" + status + '\'' +
            '}';
    }
}
