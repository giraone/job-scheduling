package com.giraone.jobs.events;

import java.time.Instant;

/**
 * All events after "accepted" with their "status".
 */
public abstract class AbstractJobStatusChangedEvent extends AbstractJobEvent {

    protected String status;

    protected AbstractJobStatusChangedEvent() {
    }

    protected AbstractJobStatusChangedEvent(String id, String processKey, Instant jobAcceptedTimestamp,
                                            Instant eventTimestamp, String payload, String status) {
        super(id, processKey, jobAcceptedTimestamp, eventTimestamp, payload);
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
