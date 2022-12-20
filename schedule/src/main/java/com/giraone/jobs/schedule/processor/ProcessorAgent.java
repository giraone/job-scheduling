package com.giraone.jobs.schedule.processor;

import com.giraone.jobs.events.AbstractAssignedJobEvent;
import com.giraone.jobs.events.JobCompletedEvent;
import com.giraone.jobs.events.JobFailedEvent;
import com.giraone.jobs.events.JobScheduledEvent;
import com.giraone.jobs.schedule.constants.UtilsAndConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class ProcessorAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorAgent.class);

    private static final Random RANDOM = new Random();

    public AbstractAssignedJobEvent streamProcess(JobScheduledEvent jobScheduledEvent) {

        LOGGER.debug("<<<< ProcessorAgent {}", jobScheduledEvent);

        UtilsAndConstants.simulationModeSleep();

        if (jobScheduledEvent.getId() == null) {
            throw new IllegalArgumentException("Forced runtime exception because id is null");
        }

        if (RANDOM.nextInt(100) == 0) {
            LOGGER.warn("Job {} of {} in agent '{}' FAILED!",
                jobScheduledEvent.getId(), jobScheduledEvent.getProcessKey(), jobScheduledEvent.getAgentKey());
            return new JobFailedEvent(jobScheduledEvent,
                "Job " + jobScheduledEvent.getId() + " of " + jobScheduledEvent.getProcessKey() + " failed!");
        } else {
            LOGGER.debug("Job {} of {} in agent '{}' SUCCEEDED.",
                jobScheduledEvent.getId(), jobScheduledEvent.getProcessKey(), jobScheduledEvent.getAgentKey());
            return new JobCompletedEvent(jobScheduledEvent, generateLinkToResult(jobScheduledEvent));
        }
    }

    private String generateLinkToResult(JobScheduledEvent jobScheduledEvent) {
        return "https://link/" + jobScheduledEvent.getMessageKey() + "-" + System.currentTimeMillis();
    }
}
