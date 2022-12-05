package com.giraone.jobs.schedule.model;

import com.giraone.jobs.events.JobPausedEvent;

public class StillPausedException extends RuntimeException {

    private final JobPausedEvent messageObject;

    public StillPausedException(String logMessage, JobPausedEvent messageObject) {
        super(logMessage);
        this.messageObject = messageObject;
    }

    public JobPausedEvent getMessageObject() {
        return messageObject;
    }
}
