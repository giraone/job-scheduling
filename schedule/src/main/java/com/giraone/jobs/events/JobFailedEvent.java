package com.giraone.jobs.events;

import java.io.Serial;
import java.time.Instant;

public class JobFailedEvent extends AbstractJobStatusChangedEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    public JobFailedEvent() {
    }

    public JobFailedEvent(JobScheduledEvent jobScheduledEvent, String result) {
        this(jobScheduledEvent.getId(), jobScheduledEvent.getProcessKey(), Instant.now(), result);
    }

    public JobFailedEvent(long id, String processKey, Instant eventTimestamp, String payload) {
        super(id, processKey, eventTimestamp, payload, "FAILED");
    }

    @Override
    public String getStatus() {
        return "FAILED";
    }
}