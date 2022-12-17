package com.giraone.jobs.events;

import java.io.Serial;
import java.time.Instant;

public class JobNotifiedEvent extends AbstractJobStatusChangedEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    public JobNotifiedEvent() {
    }

    public JobNotifiedEvent(JobCompletedEvent jobCompletedEvent) {
        this(jobCompletedEvent.getId(), jobCompletedEvent.getProcessKey(), Instant.now(), jobCompletedEvent.getPayload());
    }

    public JobNotifiedEvent(String id, String processKey, Instant eventTimestamp, String payload) {
        super(id, processKey, eventTimestamp, payload, "NOTIFIED");
    }

    @Override
    public String getStatus() {
        return "NOTIFIED";
    }
}