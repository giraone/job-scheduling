package com.giraone.jobs.events;

import java.io.Serial;
import java.time.Instant;

public class JobCompletedEvent extends AbstractJobStatusChangedEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    public JobCompletedEvent() {
    }

    public JobCompletedEvent(JobScheduledEvent jobScheduledEvent, String result) {
        this(jobScheduledEvent.getId(), jobScheduledEvent.getProcessKey(), Instant.now(), result);
    }

    public JobCompletedEvent(long id, String processKey, Instant eventTimestamp, String payload) {
        super(id, processKey, eventTimestamp, payload, "COMPLETED");
    }

    @Override
    public String getStatus() {
        return "COMPLETED";
    }
}