package com.giraone.jobs.schedule.processor;

import com.giraone.jobs.events.JobCompletedEvent;
import com.giraone.jobs.events.JobNotifiedEvent;
import com.giraone.jobs.schedule.constants.UtilsAndConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProcessorNotify {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorNotify.class);

    public JobNotifiedEvent streamProcess(JobCompletedEvent jobCompletedEvent) {

        LOGGER.debug(">>> ProcessorNotify.streamProcess {}", jobCompletedEvent);

        UtilsAndConstants.simulationModeSleep();

        if (jobCompletedEvent.getId() == null) {
            throw new IllegalArgumentException("Forced runtime exception because id is null");
        }

        return new JobNotifiedEvent(jobCompletedEvent);
    }
}
