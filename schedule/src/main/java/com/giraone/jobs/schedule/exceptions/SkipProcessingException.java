package com.giraone.jobs.schedule.exceptions;

public class SkipProcessingException extends RuntimeException {

    public SkipProcessingException(String message) {
        super(message);
    }

    public SkipProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
