package com.giraone.jobs.events;

import java.io.Serial;
import java.time.Instant;

public class JobScheduledEvent extends AbstractAssignedJobEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    public JobScheduledEvent() {
    }

    public JobScheduledEvent(JobAcceptedEvent jobAcceptedEvent, String agentKey) {
        this(jobAcceptedEvent.getId(), jobAcceptedEvent.getProcessKey(), Instant.now(), jobAcceptedEvent.getPayload(), agentKey);
    }

    public JobScheduledEvent(JobPausedEvent jobPausedEvent, String agentKey) {
        this(jobPausedEvent.getId(), jobPausedEvent.getProcessKey(), Instant.now(), jobPausedEvent.getPayload(), agentKey);
    }

    public JobScheduledEvent(String id, String processKey, Instant eventTimestamp, String payload, String agentKey) {
        super(id, processKey, eventTimestamp, payload, "SCHEDULED", agentKey);
    }

    @Override
    public String getStatus() {
        return "SCHEDULED";
    }
}