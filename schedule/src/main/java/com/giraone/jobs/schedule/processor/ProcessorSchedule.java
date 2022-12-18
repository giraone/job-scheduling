package com.giraone.jobs.schedule.processor;

import com.giraone.jobs.events.AbstractJobEvent;
import com.giraone.jobs.events.JobAcceptedEvent;
import com.giraone.jobs.events.JobPausedEvent;
import com.giraone.jobs.events.JobScheduledEvent;
import com.giraone.jobs.schedule.constants.UtilsAndConstants;
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

    public AbstractJobEvent streamProcess(JobAcceptedEvent jobAcceptedEvent) {

        LOGGER.debug(">>> ProcessorSchedule.streamProcess {}", jobAcceptedEvent);

        UtilsAndConstants.simulationModeSleep();

        if (jobAcceptedEvent.getId() == null) {
            throw new IllegalArgumentException("Forced runtime exception because ID-NULL");
        }

        final String processKey = jobAcceptedEvent.getProcessKey();
        final String pausedBucketKey = pausedDecider.isProcessPaused(processKey);
        LOGGER.info(">>> ProcessorSchedule.streamProcess {} {} pausedBucketKey={}",
            jobAcceptedEvent.getId(), jobAcceptedEvent.getProcessKey(), pausedBucketKey);
        if (pausedBucketKey != null) {
            final JobPausedEvent jobPausedEvent = new JobPausedEvent(pausedBucketKey, jobAcceptedEvent);
            LOGGER.info(">>> Process {} is paused. Moving job {} to paused bucket {} topic!",
                processKey, jobPausedEvent.getMessageKey(), jobPausedEvent.getBucketSuffix());
            return jobPausedEvent;
        } else {
            return new JobScheduledEvent(jobAcceptedEvent);
        }
    }
}
