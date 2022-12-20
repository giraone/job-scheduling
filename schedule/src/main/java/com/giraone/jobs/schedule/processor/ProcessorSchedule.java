package com.giraone.jobs.schedule.processor;

import com.giraone.jobs.events.AbstractJobStatusChangedEvent;
import com.giraone.jobs.events.JobAcceptedEvent;
import com.giraone.jobs.events.JobPausedEvent;
import com.giraone.jobs.events.JobScheduledEvent;
import com.giraone.jobs.schedule.constants.UtilsAndConstants;
import com.giraone.jobs.schedule.service.PausedDecider;
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

    public AbstractJobStatusChangedEvent streamProcess(JobAcceptedEvent jobAcceptedEvent) {

        LOGGER.debug("<<<< ProcessorSchedule {}", jobAcceptedEvent);

        UtilsAndConstants.simulationModeSleep();

        if (jobAcceptedEvent.getId() == null) {
            throw new IllegalArgumentException("Forced runtime exception because ID-NULL");
        }

        final String processKey = jobAcceptedEvent.getProcessKey();
        final String pausedBucketKey = pausedDecider.getBucketIfProcessPaused(processKey);

        if (pausedBucketKey != null) {
            final JobPausedEvent jobPausedEvent = new JobPausedEvent(pausedBucketKey, jobAcceptedEvent);
            LOGGER.info(">>> PAUSED        {} of {} to bucket '{}'!",
                jobPausedEvent.getId(), processKey, jobPausedEvent.getPausedBucketKey());
            return jobPausedEvent;
        } else {
            final String agentKey = pausedDecider.getAgentKeyForProcess(processKey);
            LOGGER.info(">>> SCHEDULING    {} of {} to agent '{}'",
                jobAcceptedEvent.getId(), processKey, agentKey);
            return new JobScheduledEvent(jobAcceptedEvent, agentKey);
        }
    }
}
