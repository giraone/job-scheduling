package com.giraone.jobs.events;

import java.time.Instant;

/**
 * Alle events, for which the agent is known.
 */
public abstract class AbstractAssignedJobEvent extends AbstractJobStatusChangedEvent {

    protected String agentKey;

    protected AbstractAssignedJobEvent() {
    }

    protected AbstractAssignedJobEvent(String id, String processKey, Instant jobAcceptedTimestamp,
                                       Instant eventTimestamp, String payload,
                                       String status, String agentKey) {
        super(id, processKey, jobAcceptedTimestamp, eventTimestamp, payload, status);
        this.agentKey = agentKey;
    }

    public String getAgentKey() {
        return agentKey;
    }

    public void setAgentKey(String agentKey) {
        this.agentKey = agentKey;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
            "id=" + id +
            ", eventTimestamp=" + eventTimestamp +
            ", processKey='" + processKey + '\'' +
            ", payload='" + payload + '\'' +
            ", status='" + status + '\'' +
            ", agentKey='" + agentKey + '\'' +
            '}';
    }
}
