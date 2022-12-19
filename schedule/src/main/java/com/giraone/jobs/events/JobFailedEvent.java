package com.giraone.jobs.events;

import java.io.Serial;
import java.time.Instant;

public class JobFailedEvent extends AbstractAssignedJobEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    public JobFailedEvent() {
    }

    public JobFailedEvent(JobScheduledEvent jobScheduledEvent, String result) {
        this(jobScheduledEvent.getId(), jobScheduledEvent.getProcessKey(), jobScheduledEvent.getJobAcceptedTimestamp(),
            Instant.now(), result, jobScheduledEvent.getAgentKey());
    }

    public JobFailedEvent(String id, String processKey, Instant jobAcceptedTimestamp, Instant eventTimestamp, String payload, String agentKey) {
        super(id, processKey, jobAcceptedTimestamp, eventTimestamp, payload, "FAILED", agentKey);
    }

    @Override
    public String getStatus() {
        return "FAILED";
    }
}