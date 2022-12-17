package com.giraone.jobs.events;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serial;
import java.time.Instant;

public class JobScheduledEvent extends AbstractJobStatusChangedEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    public JobScheduledEvent() {
    }

    public JobScheduledEvent(JobAcceptedEvent jobAcceptedEvent) {
        this(jobAcceptedEvent.getId(), jobAcceptedEvent.getProcessKey(), Instant.now(), jobAcceptedEvent.getPayload());
    }

    public JobScheduledEvent(JobPausedEvent jobPausedEvent) {
        this(jobPausedEvent.getId(), jobPausedEvent.getProcessKey(), Instant.now(), jobPausedEvent.getPayload());
    }

    public JobScheduledEvent(String id, String processKey, Instant eventTimestamp, String payload) {
        super(id, processKey, eventTimestamp, payload, "SCHEDULED");
    }

    @Override
    public String getStatus() {
        return "SCHEDULED";
    }

    // n:m mapping of process key to agent
    @JsonIgnore
    public String getAgentSuffix() {
        return this.getProcessKey();
    }
}