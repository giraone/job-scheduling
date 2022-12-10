package com.giraone.jobs.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.giraone.jobs.common.CustomInstantSerializer;
import com.giraone.jobs.common.TolerantInstantDeserializer;

import java.io.Serializable;
import java.time.Instant;

public abstract class AbstractJobEvent implements Serializable {

    protected long id;
    @JsonDeserialize(using = TolerantInstantDeserializer.class)
    @JsonSerialize(using = CustomInstantSerializer.class)
    protected Instant eventTimestamp;
    protected String processKey;
    protected String payload;

    protected AbstractJobEvent() {
    }

    protected AbstractJobEvent(long id, String processKey, Instant eventTimestamp, String payload) {
        this.id = id;
        this.processKey = processKey;
        this.eventTimestamp = eventTimestamp;
        this.payload = payload;
    }

    @JsonIgnore
    public String getMessageKey() {
        return String.format("%8x", id);
    }

    public abstract String getStatus();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
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
            ", eventTimestamp=" + eventTimestamp +
            ", processKey='" + processKey + '\'' +
            ", payload='" + payload + '\'' +
            '}';
    }
}