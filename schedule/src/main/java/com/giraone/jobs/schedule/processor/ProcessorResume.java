package com.giraone.jobs.schedule.processor;

import com.giraone.jobs.events.JobPausedEvent;
import com.giraone.jobs.events.JobScheduledEvent;
import com.giraone.jobs.schedule.constants.UtilsAndConstants;
import com.giraone.jobs.schedule.service.PausedDecider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProcessorResume {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorResume.class);

    private final PausedDecider pausedDecider;

    public ProcessorResume(PausedDecider pausedDecider) {
        this.pausedDecider = pausedDecider;
    }

    public Optional<JobScheduledEvent> streamProcess(JobPausedEvent jobPausedEvent) {

        LOGGER.debug(">>> ProcessorResume.streamProcess {}", jobPausedEvent);

        UtilsAndConstants.simulationModeSleep();

        if (jobPausedEvent.getId() == null) {
            throw new IllegalArgumentException("Forced runtime exception because id is null");
        }

        final String processKey = jobPausedEvent.getProcessKey();
        final String pausedBucketKey = pausedDecider.getBucketIfProcessPaused(processKey);
        LOGGER.info(">>> ProcessorResume.streamProcess {} {} pausedBucketKey={}",
            jobPausedEvent.getId(), jobPausedEvent.getProcessKey(), pausedBucketKey);
        if (pausedBucketKey != null) {
            LOGGER.info(">>> ProcessorResume.streamProcess keeping {} of {} paused", jobPausedEvent.getId(), processKey);
            return Optional.empty();
        } else {
            LOGGER.info(">>> ProcessorResume.streamProcess re-scheduling {} of {}", jobPausedEvent.getId(), processKey);
            return Optional.of(new JobScheduledEvent(jobPausedEvent));
        }
    }
}
