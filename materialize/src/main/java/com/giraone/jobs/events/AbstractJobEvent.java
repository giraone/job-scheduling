package com.giraone.jobs.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.giraone.jobs.common.CustomInstantSerializer;
import com.giraone.jobs.common.TolerantInstantDeserializer;
import com.github.f4b6a3.tsid.Tsid;

import java.io.Serializable;
import java.time.Instant;

public abstract class AbstractJobEvent implements Serializable {

    private long id;
    @JsonDeserialize(using = TolerantInstantDeserializer.class)
    @JsonSerialize(using = CustomInstantSerializer.class)
    private Instant eventTimestamp;
    private String processKey;

    protected AbstractJobEvent() {
    }

    protected AbstractJobEvent(long id, String processKey, Instant eventTimestamp) {
        this.id = id;
        this.processKey = processKey;
        this.eventTimestamp = eventTimestamp;
    }

    @JsonIgnore
    public String getMessageKey() {
        return Tsid.from(id).toString();
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

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
            "id=" + id +
            ", eventTimestamp=" + eventTimestamp +
            ", processKey='" + processKey + '\'' +
            '}';
    }
}