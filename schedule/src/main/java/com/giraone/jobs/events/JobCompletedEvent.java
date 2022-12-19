package com.giraone.jobs.events;

import java.io.Serial;
import java.time.Instant;

public class JobCompletedEvent extends AbstractAssignedJobEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    public JobCompletedEvent() {
    }

    public JobCompletedEvent(JobScheduledEvent jobScheduledEvent, String result) {
        this(jobScheduledEvent.getId(), jobScheduledEvent.getProcessKey(), jobScheduledEvent.getJobAcceptedTimestamp(),
            Instant.now(), result, jobScheduledEvent.getAgentKey());
    }

    public JobCompletedEvent(String id, String processKey, Instant jobAcceptedTimestamp, Instant eventTimestamp, String payload, String agentKey) {
        super(id, processKey, jobAcceptedTimestamp, eventTimestamp, payload, "COMPLETED", agentKey);
    }

    @Override
    public String getStatus() {
        return "COMPLETED";
    }
}