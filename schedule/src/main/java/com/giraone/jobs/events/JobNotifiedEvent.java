package com.giraone.jobs.events;

import java.io.Serial;
import java.time.Instant;

public class JobNotifiedEvent extends AbstractAssignedJobEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    public JobNotifiedEvent() {
    }

    public JobNotifiedEvent(JobCompletedEvent jobCompletedEvent) {
        this(jobCompletedEvent.getId(), jobCompletedEvent.getProcessKey(), jobCompletedEvent.getJobAcceptedTimestamp(),
            Instant.now(), jobCompletedEvent.getPayload(), jobCompletedEvent.getAgentKey());
    }

    public JobNotifiedEvent(String id, String processKey, Instant jobAcceptedTimestamp, Instant eventTimestamp, String payload, String agentKey) {
        super(id, processKey, jobAcceptedTimestamp, eventTimestamp, payload, "NOTIFIED", agentKey);
    }

    @Override
    public String getStatus() {
        return "NOTIFIED";
    }
}