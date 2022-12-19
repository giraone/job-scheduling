package com.giraone.jobs.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.giraone.jobs.common.CustomInstantSerializer;
import com.giraone.jobs.common.TolerantInstantDeserializer;
import com.github.f4b6a3.tsid.Tsid;

import java.io.Serializable;
import java.time.Instant;

/**
 * Base class for all events.
 */
public abstract class AbstractJobEvent implements Serializable {

    protected String id;
    protected String processKey;
    @JsonDeserialize(using = TolerantInstantDeserializer.class)
    @JsonSerialize(using = CustomInstantSerializer.class)
    protected Instant jobAcceptedTimestamp;
    @JsonDeserialize(using = TolerantInstantDeserializer.class)
    @JsonSerialize(using = CustomInstantSerializer.class)
    protected Instant eventTimestamp;

    protected String payload;

    protected AbstractJobEvent() {
    }

    protected AbstractJobEvent(String id, String processKey, Instant jobAcceptedTimestamp, Instant eventTimestamp, String payload) {
        this.id = id;
        this.processKey = processKey;
        this.jobAcceptedTimestamp = jobAcceptedTimestamp;
        this.eventTimestamp = eventTimestamp;
        this.payload = payload;
    }

    @JsonIgnore
    public String getMessageKey() {
        return Tsid.from(id).toString();
    }

    public abstract String getStatus();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
    }

    public Instant getJobAcceptedTimestamp() {
        return jobAcceptedTimestamp;
    }

    public void setJobAcceptedTimestamp(Instant jobAcceptedTimestamp) {
        this.jobAcceptedTimestamp = jobAcceptedTimestamp;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
            "id=" + id +
            ", processKey='" + processKey + '\'' +
            ", jobAcceptedTimestamp=" + jobAcceptedTimestamp +
            ", eventTimestamp=" + eventTimestamp +
            ", payload='" + payload + '\'' +
            '}';
    }
}