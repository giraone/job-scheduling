package com.giraone.jobs.schedule.exceptions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.giraone.jobs.schedule.constants.UtilsAndConstants;
import lombok.Generated;

/**
 * A data class for storing a message (key, value) that caused a processing exception.
 * The class contains also the error text and the stack trace.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Generated
public class DocumentedErrorOutput {

    public final String causedByKey;
    public final Object causedByMessage;
    public final String errorText;
    public final String stacktrace;

    public DocumentedErrorOutput(String causedByKey, Object causedByMessage, Exception exception) {
        this.causedByKey = causedByKey;
        this.causedByMessage = causedByMessage;
        this.errorText = exception.getMessage();
        this.stacktrace = UtilsAndConstants.convertStackTraceToString(exception);
    }
}
