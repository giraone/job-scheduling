package com.giraone.jobs.schedule.model;

import com.giraone.jobs.events.JobPausedEvent;

public class PausedException extends RuntimeException {

    private final JobPausedEvent messageObject;

    public PausedException(String logMessage, JobPausedEvent messageObject) {
        super(logMessage);
        this.messageObject = messageObject;
    }

    public JobPausedEvent getMessageObject() {
        return messageObject;
    }
}
