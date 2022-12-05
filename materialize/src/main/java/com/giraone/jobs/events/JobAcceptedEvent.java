package com.giraone.jobs.events;

import java.io.Serial;
import java.time.Instant;

public class JobAcceptedEvent extends AbstractJobEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    public JobAcceptedEvent() {
        super();
    }

    public JobAcceptedEvent(long id, String processKey, Instant eventTimestamp) {
        super(id, processKey, eventTimestamp);
    }

    @Override
    public String getStatus() {
        return "ACCEPTED";
    }
}