package com.giraone.jobs.schedule.model;

import com.giraone.jobs.events.JobFailedEvent;

public class AgentFailedException extends RuntimeException {

    private final JobFailedEvent messageObject;

    public AgentFailedException(String logMessage, JobFailedEvent messageObject) {
        super(logMessage);
        this.messageObject = messageObject;
    }

    public JobFailedEvent getMessageObject() {
        return messageObject;
    }
}
