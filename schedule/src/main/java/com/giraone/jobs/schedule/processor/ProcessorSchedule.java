package com.giraone.jobs.schedule.processor;

import com.giraone.jobs.events.JobAcceptedEvent;
import com.giraone.jobs.events.JobPausedEvent;
import com.giraone.jobs.events.JobScheduledEvent;
import com.giraone.jobs.schedule.constants.UtilsAndConstants;
import com.giraone.jobs.schedule.model.PausedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProcessorSchedule {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorSchedule.class);

    private final PausedDecider pausedDecider;

    public ProcessorSchedule(PausedDecider pausedDecider) {
        this.pausedDecider = pausedDecider;
    }

    public JobScheduledEvent streamProcess(JobAcceptedEvent jobAcceptedEvent) {

        LOGGER.debug(">>> ProcessorSchedule.streamProcess {}", jobAcceptedEvent);

        UtilsAndConstants.simulationModeSleep();

        if (jobAcceptedEvent.getId() == 0L) {
            throw new IllegalArgumentException("Forced runtime exception because ID-NULL");
        }

        String processKey = jobAcceptedEvent.getProcessKey();
        Integer pausedBucket = pausedDecider.isProcessPaused(processKey);
        if (pausedBucket != null) {
            JobPausedEvent jobPausedEvent = new JobPausedEvent(pausedBucket, jobAcceptedEvent);
            throw new PausedException("Process " + processKey + " is paused. Moving job "
                + jobAcceptedEvent.getId() + " to paused bucket " + pausedBucket + " in topic!", jobPausedEvent);
        }

        return new JobScheduledEvent(jobAcceptedEvent);
    }
}
