package com.giraone.jobs.events;

import java.io.Serial;
import java.time.Instant;

public class JobScheduledEvent extends AbstractAssignedJobEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    public JobScheduledEvent() {
    }

    public JobScheduledEvent(JobAcceptedEvent jobAcceptedEvent, String agentKey) {
        this(jobAcceptedEvent.getId(), jobAcceptedEvent.getProcessKey(), jobAcceptedEvent.getJobAcceptedTimestamp(),
            Instant.now(), jobAcceptedEvent.getPayload(), agentKey);
    }

    public JobScheduledEvent(JobPausedEvent jobPausedEvent, String agentKey) {
        this(jobPausedEvent.getId(), jobPausedEvent.getProcessKey(), jobPausedEvent.getJobAcceptedTimestamp(),
            Instant.now(), jobPausedEvent.getPayload(), agentKey);
    }

    public JobScheduledEvent(String id, String processKey, Instant jobAcceptedTimestamp,
                             Instant eventTimestamp, String payload, String agentKey) {
        super(id, processKey, jobAcceptedTimestamp, eventTimestamp, payload, "SCHEDULED", agentKey);
    }

    @Override
    public String getStatus() {
        return "SCHEDULED";
    }
}