package com.giraone.jobs.schedule.processor;

import com.giraone.jobs.events.AbstractJobEvent;
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

    public AbstractJobEvent streamProcess(JobScheduledEvent jobScheduledEvent) {

        LOGGER.debug(">>> ProcessorAgent.streamProcess {}", jobScheduledEvent);

        UtilsAndConstants.simulationModeSleep();

        if (jobScheduledEvent.getId() == 0L) {
            throw new IllegalArgumentException("Forced runtime exception because id is null");
        }

        if (RANDOM.nextInt(100) == 0) {
            LOGGER.debug("Job {} of {} failed", jobScheduledEvent.getId(), jobScheduledEvent.getProcessKey());
            return new JobFailedEvent(jobScheduledEvent,
                "Job " + jobScheduledEvent.getId() + " of " + jobScheduledEvent.getProcessKey() + " failed!");
        } else {
            LOGGER.debug("Job {} of {} succeeded", jobScheduledEvent.getId(), jobScheduledEvent.getProcessKey());
            return new JobCompletedEvent(jobScheduledEvent, generateLinkToResult(jobScheduledEvent));
        }
    }

    private String generateLinkToResult(JobScheduledEvent jobScheduledEvent) {
        return "https://link/" + jobScheduledEvent.getMessageKey() + "-" + System.currentTimeMillis();
    }
}
